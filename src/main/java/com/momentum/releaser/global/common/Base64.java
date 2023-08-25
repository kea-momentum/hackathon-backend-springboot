package com.momentum.releaser.global.common;

import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
public class Base64 {

    /**
     * Base64 인코딩된 이미지 데이터를 파일로 변환한다.
     */
    public static File getImageUrlFromBase64(String base64) throws IOException {
        String[] base64s = base64.split(",");
        String extension;

        // byte[] 타입으로 바꾸기 전에 'data:image/jpeg;base64,' 부분을 제거한다.
        switch(base64s[0]) {
            case "data:image/jpeg;base64":
                extension = ".jpeg";
                break;

            case "data:image/png;base64":
                extension = ".png";
                break;

            default:
                extension = ".jpg";
                break;
        }

        // Convert base64 string to binary data
        byte[] data = DatatypeConverter.parseBase64Binary(base64s[1]);
        LocalDateTime localDateTime = LocalDateTime.now();
        long currentTimeMills = Timestamp.valueOf(localDateTime).getTime();

        File file = new File(currentTimeMills + extension);
        log.info("file: {}", file.getName());

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
        outputStream.write(data);

        return file;
    }
}
