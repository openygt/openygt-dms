package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.service.BarcodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BarcodeServiceImpl implements BarcodeService {

    @Override
    public String generateQrCode(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return imageToBase64(image);
        } catch (Exception e) {
            log.error("生成二维码失败: {}", content, e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    @Override
    public String generateBarcode(String content, int width, int height) {
        try {
            Code128Writer writer = new Code128Writer();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 5);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return imageToBase64(image);
        } catch (Exception e) {
            log.error("生成条码失败: {}", content, e);
            throw new RuntimeException("生成条码失败", e);
        }
    }

    private String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("图片转Base64失败", e);
        }
    }
}
