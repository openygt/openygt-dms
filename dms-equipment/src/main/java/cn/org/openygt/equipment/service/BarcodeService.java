package cn.org.openygt.equipment.service;

public interface BarcodeService {
    /**
     * 生成设备二维码（Base64 PNG）
     */
    String generateQrCode(String content, int width, int height);

    /**
     * 生成设备一维条码（Base64 PNG）
     */
    String generateBarcode(String content, int width, int height);
}
