package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogProviderTest {

    private static final String CB_IMAGE_CATALOG_V2_JSON = "cb-image-catalog-v2.json";

    private static final String CB_IMAGE_CATALOG_NULL_FIELD_JSON = "cb-image-catalog-null-field.json";

    private static final String CB_IMAGE_CATALOG_VALID_JSON = "cb-image-catalog-valid.json";

    private static final String CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON = "cb-image-catalog-filter-null-images.json";

    private static final String CB_IMAGE_CATALOG_EMPTY_CLOUDBREAK_VERSIONS_JSON = "cb-image-catalog-empty-cloudbreak-versions.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES = "cb-image-catalog-without-base-images.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_HDP_IMAGES = "cb-image-catalog-without-hdp-images.json";

    private static final String CB_IMAGE_CATALOG_WITHOUT_HDF_IMAGES = "cb-image-catalog-without-hdf-images.json";

    private static final String CB_VERSION = "1.16.4";

    @InjectMocks
    private CachedImageCatalogProvider underTest;

    @Test
    public void testReadImageCatalogFromFile() throws Exception {

        String path = getPath(CB_IMAGE_CATALOG_V2_JSON);
        underTest.setEtcConfigDir(path);

        CloudbreakImageCatalogV2 catalog = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_V2_JSON);

        Assert.assertNotNull("Check that the parsed ImageCatalog not null.", catalog);
        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
        Assert.assertTrue("Check that the parsed ImageCatalog contains the desired version of Cloudbreak.", ver.isPresent());
        List<String> imageIds = ver.get().getImageIds();
        Assert.assertNotNull("Check that the parsed ImageCatalog contains the desired version of Cloudbreak with image id(s).", imageIds);
        Optional<String> imageIdOptional = imageIds.stream().findFirst();
        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image reference for the Cloudbreak version.", imageIdOptional.isPresent());
        String imageId = imageIdOptional.get();
        boolean baseImageFound = false;
        boolean hdpImageFound = false;
        boolean hdfImageFoiund = false;
        if (catalog.getImages().getBaseImages() != null) {
            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        if (catalog.getImages().getHdpImages() != null) {
            hdpImageFound = catalog.getImages().getHdpImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        if (catalog.getImages().getHdfImages() != null) {
            hdfImageFoiund = catalog.getImages().getHdfImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        boolean anyImageFoundForVersion = baseImageFound || hdpImageFound || hdfImageFoiund;
        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image for the Cloudbreak version.", anyImageFoundForVersion);
    }

    @Test
    public void testImageCatalogErrorWhenNull() throws CloudbreakImageCatalogException {
        String path = getPath(CB_IMAGE_CATALOG_NULL_FIELD_JSON);
        underTest.setEtcConfigDir(path);
        String errorMessage = getErrorMessage(CB_IMAGE_CATALOG_NULL_FIELD_JSON);

        String expected = "Missing required creator property 'images' (index 0)";
        Assert.assertTrue("Check that the 'images' field is missing", errorMessage.startsWith(expected));

    }

    private String getPath(String catalogUrl) {
        try {
            return TestUtil.getFilePath(getClass(), catalogUrl).getParent().toString();
        } catch (Exception e) {

            throw new RuntimeException(String.format("Catalog JSON cannot be read: %s", catalogUrl), e);
        }
    }

    @Test
    public void testImageCatalogValid() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_VALID_JSON);
        underTest.setEtcConfigDir(path);
        underTest.getImageCatalogV2(CB_IMAGE_CATALOG_VALID_JSON);
    }

    @Test
    public void testImageCatalogWithEmptyCloudBreakVersions() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_EMPTY_CLOUDBREAK_VERSIONS_JSON);
        underTest.setEtcConfigDir(path);

        String errorMessage = getErrorMessage(CB_IMAGE_CATALOG_EMPTY_CLOUDBREAK_VERSIONS_JSON);
        String expected = "Cloudbreak versions cannot be NULL";
        Assert.assertTrue("Check that the Cloudbreak version cannot be empty", errorMessage.startsWith(expected));
    }

    @Test
    public void testImageCatalogFilterNullImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON);
        underTest.setEtcConfigDir(path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_FILTER_NULL_IMAGES_JSON);
        Assert.assertEquals(1, imageCatalogV2.getImages().getBaseImages().get(0).getImageSetsByProvider().values().size());
    }

    @Test
    public void testImageCatalogWithoutHdfImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_HDF_IMAGES);
        underTest.setEtcConfigDir(path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_WITHOUT_HDF_IMAGES);
        Assert.assertNotNull(imageCatalogV2.getImages().getHdfImages());
    }

    @Test
    public void testImageCatalogWithoutHdpImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_HDP_IMAGES);
        underTest.setEtcConfigDir(path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_WITHOUT_HDP_IMAGES);
        Assert.assertNotNull(imageCatalogV2.getImages().getHdpImages());
    }

    @Test
    public void testImageCatalogWithoutBaseImages() throws CloudbreakImageCatalogException, IOException {
        String path = getPath(CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES);
        underTest.setEtcConfigDir(path);

        CloudbreakImageCatalogV2 imageCatalogV2 = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_WITHOUT_BASE_IMAGES);
        Assert.assertNotNull(imageCatalogV2.getImages().getBaseImages());
    }

    private String getErrorMessage(String catalogUrl) {
        String errorMessage = "";
        try {
            underTest.getImageCatalogV2(catalogUrl);
        } catch (CloudbreakImageCatalogException e) {
            errorMessage = e.getMessage();
        }

        return errorMessage;
    }

//    @Test
//    public void testReadImageCatalogFromHTTP() {
//        CloudbreakImageCatalogV2 catalog = underTest.getImageCatalogV2();
//
//        Assert.assertNotNull("Check that the parsed ImageCatalog not null.", catalog);
//        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
//        Assert.assertTrue("Check that the parsed ImageCatalog contains the desired version of Cloudbreak.", ver.isPresent());
//        List<String> imageIds = ver.get().getImageIds();
//        Assert.assertNotNull("Check that the parsed ImageCatalog contains the desired version of Cloudbreak with image id(s).", imageIds);
//        Optional<String> imageIdOptional = ver.get().getImageIds().stream().findFirst();
//        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image reference for the Cloudbreak version.", imageIdOptional.isPresent());
//        String imageId = imageIdOptional.get();
//        boolean baseImageFound = false;
//        boolean hdpImageFound = false;
//        boolean hdfImageFoiund = false;
//        if (catalog.getImages().getBaseImages() != null) {
//            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        if (catalog.getImages().getHdpImages() != null) {
//            hdpImageFound = catalog.getImages().getHdpImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        if (catalog.getImages().getHdfImages() != null) {
//            hdfImageFoiund = catalog.getImages().getHdfImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        boolean anyImageFoundForVersion = baseImageFound || hdpImageFound || hdfImageFoiund;
//        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image for the Cloudbreak version.", anyImageFoundForVersion);
//    }
}