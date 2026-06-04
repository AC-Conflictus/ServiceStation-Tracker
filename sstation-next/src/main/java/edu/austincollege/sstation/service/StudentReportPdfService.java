package edu.austincollege.sstation.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import edu.austincollege.sstation.service.StudentData.PdfReport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Renders the per-student report to PDF (TC-024 / TC-108d) using openhtmltopdf. The report is
 * authored as a Thymeleaf XHTML template; jsoup parses the rendered markup into the well-formed DOM
 * openhtmltopdf requires (it is strict where browsers are lenient).
 */
@Service
public class StudentReportPdfService {

  private final SpringTemplateEngine templateEngine;

  public StudentReportPdfService(SpringTemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  public byte[] render(PdfReport report) {
    Context ctx = new Context();
    ctx.setVariable("report", report);
    String html = templateEngine.process("pdf/student-report", ctx);

    org.jsoup.nodes.Document jsoup = org.jsoup.Jsoup.parse(html);
    jsoup.outputSettings().syntax(Syntax.xml);
    org.w3c.dom.Document w3c = new W3CDom().fromJsoup(jsoup);

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withW3cDocument(w3c, "/");
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to render student report PDF", e);
    }
  }
}
