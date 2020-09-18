package bootiful.asciidoctor.autoconfigure;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.asciidoctor.Asciidoctor;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

@Log4j2
abstract class AbstractPdfProducer implements DocumentProducer {

    private final PublicationProperties properties;

    private final Asciidoctor asciidoctor;

    // https://asciidoctor.org/docs/asciidoctor-pdf/#optimizing-the-generated-pdf

    public enum PdfOptimizerQuality {

        STANDARD, SCREEN, EBOOK, PRINTER, PREPRESS

    }

    @Data
    @RequiredArgsConstructor
    public static class PdfProducerConfiguration {

        private final PdfOptimizerQuality pdfOptimizerQuality;

        private final boolean optimize;

        private final String media;

    }

    protected AbstractPdfProducer(PublicationProperties properties, Asciidoctor asciidoctor) {
        this.properties = properties;
        this.asciidoctor = asciidoctor;
    }

    /**
     * This functionality requires {@code asciidoctor-pdf-optimize} to be on the PATH. See
     * <a href=
     * "https://asciidoctor.org/docs/asciidoctor-pdf/#optimizing-the-generated-pdf"> this
     * documentation on the Asciidoctor PDF optimizer</a>. You must have Ghostscript
     * command {@code  gs} and the {@code rghost} gem installed to use it.
     */
    @SneakyThrows
    @Deprecated
    protected File[] optimizeOutputPdf(File file) {
        var configuration = getPdfProducerConfiguration();
        if (!configuration.isOptimize()) {
            return new File[]{file};
        }

        // ok so the original file we get will be overwritten so we need to copy it and
        // then use the copy to pass to the optimizer
        var optimizedPdfFile = new File(file.getParentFile(), "index-" + configuration.getMedia() + "-optimized.pdf");
        if (optimizedPdfFile.exists()) {
            optimizedPdfFile.delete();
        }

        bootiful.asciidoctor.autoconfigure.FileCopyUtils.copy(file, optimizedPdfFile);

        if (log.isDebugEnabled()) {
            log.debug(
                    "pdf.media=" + this.properties.getPdf() + " configuration.getMedia()=" + configuration.getMedia());
        }

        var process = new ProcessBuilder()
                .command( "asciidoctor-pdf-optimize", file.getAbsolutePath())
                .inheritIO()
                .directory( file.getParentFile())
                .start() ;

        var exitCode =  process.waitFor();

/*
        var process = Runtime.getRuntime() //
                .exec("asciidoctor-pdf-optimize "
                        + file.getAbsolutePath() *//*
                 * todo make this work again +
                 * " --quality " + configuration.
                 * getPdfOptimizerQuality().name().
                 * toLowerCase()
                 *//*
                );
        var exitCode = process.waitFor();*/
        if (log.isDebugEnabled()) {
            log.debug("exit code: " + exitCode);
            log.error("error: " + FileCopyUtils.copyToString(new InputStreamReader(process.getErrorStream())));
            log.debug("input: " + FileCopyUtils.copyToString(new InputStreamReader(process.getInputStream())));
        }
        return new File[]{file, optimizedPdfFile};
    }

    /**
     * contains the subclass specific customizations of how the
     * {@link AbstractPdfProducer} should do its work.
     */
    protected abstract PdfProducerConfiguration getPdfProducerConfiguration();

    @Override
    public File[] produce() {
        var optimize = this.getPdfProducerConfiguration().isOptimize();
        var media = this.getPdfProducerConfiguration().getMedia();
        var files = new ArrayList<File>();
        var regularFile = this.producePdf(media, new File(this.properties.getRoot(), "index-" + media.toLowerCase() + ".pdf"));
        files.add(regularFile);
        if (optimize) {
            File compressedFile = new File(this.properties.getRoot(), "index-optimized-" + media.toLowerCase() + ".pdf");
            bootiful.asciidoctor.autoconfigure.FileCopyUtils.copy(regularFile, compressedFile);

            optimizeOutputPdf( compressedFile);

            files.add ( compressedFile) ;

        }
        return files.toArray(new File[0]);
    }

    private File producePdf(String media, File file) {
        var bookName = this.properties.getBookName();
        var indexAdoc = getIndexAdoc(this.properties.getRoot());
        var pdf = this.properties.getPdf();
        var attributesBuilder = this.buildCommonAttributes(bookName, pdf.getIsbn(), indexAdoc)
                .attribute("idseparator", "-") //
                .imagesDir("images") //
                .attribute("media", media) //
                .attribute("code", this.properties.getCode().getAbsolutePath()) //
                .attribute("icons", "font") //
                .attribute("pdf-style", media) //
                .attribute("idprefix") //
                .attribute("project-version", "2.0.0-SNAPSHOT") //
                .attribute("subject", bookName) //
                .attribute("project-name", bookName) //
                .attribute("pdfmarks")//
                .attribute("notitle")//
                .attribute("pdf-stylesdir", pdf.getStyles().getAbsolutePath()) //
                .attribute("pdf-fontsdir", pdf.getFonts().getAbsolutePath());


        var optionsBuilder = this.buildCommonOptions("pdf", attributesBuilder).docType("book").toFile(file);
        asciidoctor.convertFile(this.getIndexAdoc(this.properties.getRoot()), optionsBuilder);
        log.info("inside " + this.getClass().getName() + " & just created " + file.getAbsolutePath() + '.');
        return file;
    }

}
