package app.sewain.media;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Decode raster uploads or validate static SVG; filenames and MIME never determine the format. */
final class SafeImageUpload {
    private static final int MAX_INPUT=5*1024*1024,MAX_OUTPUT=20*1024*1024;
    static MultipartFile normalize(MultipartFile file,boolean pngOnly) {
        if(file==null||file.isEmpty())throw new ValidationException(ErrorCode.MEDIA_EMPTY);
        if(file.getSize()>MAX_INPUT)throw invalid();
        try(var input=file.getInputStream()){
            var bytes=input.readNBytes(MAX_INPUT+1);
            if(bytes.length>MAX_INPUT)throw invalid();
            try(var imageInput=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
                var readers=ImageIO.getImageReaders(imageInput);
                if(!readers.hasNext()){
                    if(pngOnly)throw invalid();
                    return new Image(file.getName(),safeFilename(file)+".svg","image/svg+xml",SafeSvgUpload.normalize(bytes));
                }
                var reader=readers.next();
                try{
                    var format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!Set.of("png","jpeg","jpg","gif","webp").contains(format)||(pngOnly&&!format.equals("png")))throw invalid();
                    reader.setInput(imageInput,true,true);
                    int width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<1||height<1||width>8192||height>8192||(long)width*height>16_000_000)throw invalid();
                    BufferedImage image=reader.read(0);
                    if(image==null)throw invalid();
                    var output=new ByteArrayOutputStream();
                    var outputFormat=Set.of("jpeg","jpg").contains(format)?"jpeg":"png";
                    if(!ImageIO.write(image,outputFormat,output))throw invalid();
                    if(output.size()>MAX_OUTPUT)throw invalid();
                    return new Image(file.getName(),safeFilename(file)+(outputFormat.equals("jpeg")?".jpg":".png"),"image/"+outputFormat,output.toByteArray());
                }finally{reader.dispose();}
            }
        }catch(IOException|IllegalArgumentException error){throw invalid();}
    }
    private static String safeFilename(MultipartFile file) {
        var original=Optional.ofNullable(file.getOriginalFilename()).orElse("image").replace('\\','/');
        original=original.substring(original.lastIndexOf('/')+1).replaceFirst("\\.[^.]*$","");
        original=original.replaceAll("[^\\p{L}\\p{N} ._-]","_");
        if(original.isBlank())original="image";
        return original.substring(0,Math.min(240,original.length()));
    }
    private static ValidationException invalid(){return new ValidationException(ErrorCode.MEDIA_INVALID_TYPE);}
    private record Image(String name,String originalFilename,String contentType,byte[] bytes) implements MultipartFile {
        public String getName(){return name;}public String getOriginalFilename(){return originalFilename;}public String getContentType(){return contentType;}
        public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes.clone();}
        public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
        public void transferTo(File destination)throws IOException{Files.write(destination.toPath(),bytes);}
    }
}
