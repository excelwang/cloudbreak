package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class TemplateValidator {

    @Inject
    private CloudParameterService cloudParameterService;

    private final Supplier<Map<Platform, Collection<VmType>>> virtualMachines =
            Suppliers.memoize(() -> cloudParameterService.getVmtypes(null, true).getVirtualMachines());

    private final Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters =
            Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    public void validateTemplateRequest(Template value) {
        if (StringUtils.isEmpty(value.getInstanceType())) {
            validateCustomInstanceType(value);
        } else {
            VmType vmType = null;
            VolumeParameterType volumeParameterType = null;
            Platform platform = Platform.platform(value.cloudPlatform());
            Map<Platform, Collection<VmType>> machines = virtualMachines.get();
            if (machines.containsKey(platform) && !machines.get(platform).isEmpty()) {
                for (VmType type : machines.get(platform)) {
                    if (type.value().equals(value.getInstanceType())) {
                        vmType = type;
                    }
                }
                if (vmType == null) {
                    throw new BadRequestException(
                            String.format("The '%s' instance type isn't supported by '%s' platform", value.getInstanceType(), platform.value()));
                }
            }
            Map<Platform, Map<String, VolumeParameterType>> disks = diskMappings.get();
            if (disks.containsKey(platform) && !disks.get(platform).isEmpty()) {
                Map<String, VolumeParameterType> map = disks.get(platform);
                volumeParameterType = map.get(value.getVolumeType());
                if (volumeParameterType == null) {
                    throw new BadRequestException(
                            String.format("The '%s' volume type isn't supported by '%s' platform", value.getVolumeType(), platform.value()));
                }
            }

            validateVolume(value, vmType, platform, volumeParameterType);
        }
    }

    private void validateCustomInstanceType(Template template) {
        Map<String, Object> params = template.getAttributes().getMap();
        Platform platform = Platform.platform(template.cloudPlatform());
        PlatformParameters pps = platformParameters.get().get(platform);
        if (pps != null) {
            Boolean customInstanceType = pps.specialParameters().getSpecialParameters().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE);
            if (BooleanUtils.isTrue(customInstanceType)) {
                if (params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS) == null
                        || params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY) == null) {
                    throw new BadRequestException(String.format("Missing 'cpus' or 'memory' param for custom instancetype on %s platform",
                            template.cloudPlatform()));
                }
            } else {
                throw new BadRequestException(String.format("Custom instancetype is not supported on %s platform", template.cloudPlatform()));
            }
        }
    }

    private void validateVolume(Template value, VmType vmType, Platform platform, VolumeParameterType volumeParameterType) {
        validateVolumeType(value, platform);
        validateVolumeCount(value, vmType, volumeParameterType);
        validateVolumeSize(value, vmType, volumeParameterType);
        validateMaximumVolumeSize(value, vmType);
    }

    private void validateMaximumVolumeSize(Template value, VmType vmType) {
        if (vmType != null) {
            String maxSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB);
            if (maxSize != null) {
                int fullSize = value.getVolumeSize() * value.getVolumeCount();
                if (Integer.parseInt(maxSize) < fullSize) {
                    throw new BadRequestException(
                            String.format("The %s platform does not support %s Gb full volume size. The maximum size of disks could be %s Gb.",
                                    value.cloudPlatform(), fullSize, maxSize));
                }
            }
        }
    }

    private void validateVolumeType(Template value, Platform platform) {
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            if (!diskTypes.get(platform).contains(diskType)) {
                throw new BadRequestException(String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value()));
            }
        }
    }

    private void validateVolumeCount(Template value, VmType vmType, VolumeParameterType volumeParameterType) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount())) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                if (value.getVolumeCount() > config.maximumNumber()) {
                    throw new BadRequestException(String.format("Max allowed volume count for '%s': %s", vmType.value(), config.maximumNumber()));
                } else if (value.getVolumeCount() < config.minimumNumber()) {
                    throw new BadRequestException(String.format("Min allowed volume count for '%s': %s", vmType.value(), config.minimumNumber()));
                }
            } else {
                throw new BadRequestException(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private void validateVolumeSize(Template value, VmType vmType, VolumeParameterType volumeParameterType) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount())) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                if (value.getVolumeSize() > config.maximumSize()) {
                    throw new BadRequestException(String.format("Max allowed volume size for '%s': %s", vmType.value(), config.maximumSize()));
                } else if (value.getVolumeSize() < config.minimumSize()) {
                    throw new BadRequestException(String.format("Min allowed volume size for '%s': %s", vmType.value(), config.minimumSize()));
                }
            } else {
                throw new BadRequestException(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private boolean needToCheckVolume(VolumeParameterType volumeParameterType, Object value) {
        return volumeParameterType != VolumeParameterType.EPHEMERAL || value != null;
    }
}
