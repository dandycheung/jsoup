package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 Tests for Document.

 @author Jonathan Hedley, jonathan@hedley.net */
public class DocumentTest {
    private static final String charsetUtf8 = "UTF-8";
    private static final String charsetIso8859 = "ISO-8859-1";


    @Test public void setTextPreservesDocumentStructure() {
        Document doc = Jsoup.parse("<p>Hello</p>");
        doc.text("Replaced");
        assertEquals("Replaced", doc.text());
        assertEquals("Replaced", doc.body().text());
        assertEquals(1, doc.select("head").size());
    }

    @Test public void testTitles() {
        Document noTitle = Jsoup.parse("<p>Hello</p>");
        Document withTitle = Jsoup.parse("<title>First</title><title>Ignore</title><p>Hello</p>");

        assertEquals("", noTitle.title());
        noTitle.title("Hello");
        assertEquals("Hello", noTitle.title());
        assertEquals("Hello", noTitle.select("title").first().text());

        assertEquals("First", withTitle.title());
        withTitle.title("Hello");
        assertEquals("Hello", withTitle.title());
        assertEquals("Hello", withTitle.select("title").first().text());

        Document normaliseTitle = Jsoup.parse("<title>   Hello\nthere   \n   now   \n");
        assertEquals("Hello there now", normaliseTitle.title());
    }

    @Test public void testOutputEncoding() {
        Document doc = Jsoup.parse("<p title=π>π & < > </p>");
        // default is utf-8
        assertEquals("<p title=\"π\">π &amp; &lt; &gt;</p>", doc.body().html());
        assertEquals("UTF-8", doc.outputSettings().charset().name());

        doc.outputSettings().charset("ascii");
        assertEquals(Entities.EscapeMode.base, doc.outputSettings().escapeMode());
        assertEquals("<p title=\"&#x3c0;\">&#x3c0; &amp; &lt; &gt;</p>", doc.body().html());

        doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
        assertEquals("<p title=\"&pi;\">&pi; &amp; &lt; &gt;</p>", doc.body().html());
    }

    @Test public void testXhtmlReferences() {
        Document doc = Jsoup.parse("&lt; &gt; &amp; &quot; &apos; &times;");
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        assertEquals("&lt; &gt; &amp; \" ' ×", doc.body().html());
    }

    @Test public void testNormalisesStructure() {
        Document doc = Jsoup.parse("<html><head><script>one</script><noscript><p>two</p></noscript></head><body><p>three</p></body><p>four</p></html>");
        assertEquals("<html><head><script>one</script><noscript>&lt;p&gt;two</noscript></head><body><p>three</p><p>four</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void accessorsWillNormalizeStructure() {
        Document doc = new Document("");
        assertEquals("", doc.html());

        Element body = doc.body();
        assertEquals("body", body.tagName());
        Element head = doc.head();
        assertEquals("head", head.tagName());
        assertEquals("<html><head></head><body></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void accessorsAreCaseInsensitive() {
        Parser parser = Parser.htmlParser().settings(ParseSettings.preserveCase);
        Document doc = parser.parseInput("<!DOCTYPE html><HTML><HEAD><TITLE>SHOUTY</TITLE></HEAD><BODY>HELLO</BODY></HTML>", "");

        Element body = doc.body();
        assertEquals("BODY", body.tagName());
        assertEquals("body", body.normalName());
        Element head = doc.head();
        assertEquals("HEAD", head.tagName());
        assertEquals("body", body.normalName());

        Element root = doc.selectFirst("html");
        assertEquals("HTML", root.tagName());
        assertEquals("html", root.normalName());
        assertEquals("SHOUTY", doc.title());
    }

    @Test public void testClone() {
        Document doc = Jsoup.parse("<title>Hello</title> <p>One<p>Two");
        Document clone = doc.clone();
        assertNotSame(doc, clone);
        assertTrue(doc.hasSameValue(clone));
        assertSame(doc.parser(), clone.parser());
        assertNotSame(doc.outputSettings(), clone.outputSettings());

        assertEquals("<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>", TextUtil.stripNewlines(clone.html()));
        clone.title("Hello there");
        assertFalse(doc.hasSameValue(clone));
        clone.expectFirst("p").text("One more").attr("id", "1");
        assertEquals("<html><head><title>Hello there</title></head><body><p id=\"1\">One more</p><p>Two</p></body></html>", TextUtil.stripNewlines(clone.html()));
        assertEquals("<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test void testBasicIndent() {
        Document doc = Jsoup.parse("<title>Hello</title> <p>One\n<p>Two\n");
        String expect = "<html>\n <head>\n  <title>Hello</title>\n </head>\n <body>\n  <p>One</p>\n  <p>Two</p>\n </body>\n</html>";
        String html = doc.html();
        assertEquals(expect, html);
    }

    @Test public void testClonesDeclarations() {
        Document doc = Jsoup.parse("<!DOCTYPE html><html><head><title>Doctype test");
        Document clone = doc.clone();

        assertEquals(doc.html(), clone.html());
        assertEquals("<!doctype html><html><head><title>Doctype test</title></head><body></body></html>",
                TextUtil.stripNewlines(clone.html()));
    }

    @Test public void testLocation() throws IOException {
        // tests location vs base href
        File in = ParseTest.getFile("/htmltests/basehref.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://example.com/");
        String location = doc.location();
        String baseUri = doc.baseUri();
        assertEquals("http://example.com/", location);
        assertEquals("https://example.com/path/file.html?query", baseUri);
        assertEquals("./anotherfile.html", doc.expectFirst("a").attr("href"));
        assertEquals("https://example.com/path/anotherfile.html", doc.expectFirst("a").attr("abs:href"));
    }

    @Test public void testLocationFromString() {
        Document doc = Jsoup.parse("<p>Hello");
        assertEquals("", doc.location());
    }

    @Test public void testHtmlAndXmlSyntax() {
        String h = "<!DOCTYPE html><body><img async checked='checked' src='&<>\"'>&lt;&gt;&amp;&quot;<foo />bar";
        Parser parser = Parser.htmlParser();
        parser.tagSet().valueOf("foo", Parser.NamespaceHtml).set(Tag.SelfClose); // customize foo to allow self close
        Document doc = Jsoup.parse(h, parser);

        doc.outputSettings().syntax(Syntax.html);
        assertEquals("<!doctype html>\n" +
                "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <img async checked src=\"&amp;&lt;&gt;&quot;\">&lt;&gt;&amp;\"<foo></foo>bar\n" + // html won't include self-closing
                " </body>\n" +
                "</html>", doc.html());

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        assertEquals("<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <img async=\"\" checked=\"checked\" src=\"&amp;&lt;&gt;&quot;\" />&lt;&gt;&amp;\"<foo />bar\n" + // xml will
                " </body>\n" +
                "</html>", doc.html());
    }

    @Test public void htmlParseDefaultsToHtmlOutputSyntax() {
        Document doc = Jsoup.parse("x");
        assertEquals(Syntax.html, doc.outputSettings().syntax());
    }

    @Test public void testHtmlAppendable() {
    	String htmlContent = "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>";
    	Document document = Jsoup.parse(htmlContent);
    	OutputSettings outputSettings = new OutputSettings();

    	outputSettings.prettyPrint(false);
    	document.outputSettings(outputSettings);
    	assertEquals(htmlContent, document.html(new StringWriter()).toString());
    }

    @Test public void testOverflowClone() {
        StringBuilder sb = new StringBuilder();
        sb.append("<head><base href='https://jsoup.org/'>");
        for (int i = 0; i < 100000; i++) {
            sb.append("<div>");
        }
        sb.append("<p>Hello <a href='/example.html'>there</a>");

        Document doc = Jsoup.parse(sb.toString());

        String expectedLink = "https://jsoup.org/example.html";
        assertEquals(expectedLink, doc.selectFirst("a").attr("abs:href"));
        Document clone = doc.clone();
        doc.hasSameValue(clone);
        assertEquals(expectedLink, clone.selectFirst("a").attr("abs:href"));
    }

    @Test public void DocumentsWithSameContentAreEqual() {
        Document docA = Jsoup.parse("<div/>One");
        Document docB = Jsoup.parse("<div/>One");
        Document docC = Jsoup.parse("<div/>Two");

        assertNotEquals(docA, docB);
        assertEquals(docA, docA);
        assertEquals(docA.hashCode(), docA.hashCode());
        assertNotEquals(docA.hashCode(), docC.hashCode());
    }

    @Test public void DocumentsWithSameContentAreVerifiable() {
        Document docA = Jsoup.parse("<div/>One");
        Document docB = Jsoup.parse("<div/>One");
        Document docC = Jsoup.parse("<div/>Two");

        assertTrue(docA.hasSameValue(docB));
        assertFalse(docA.hasSameValue(docC));
    }

    @Test
    public void testMetaCharsetUpdateUtf8() {
        final Document doc = createHtmlDocument("changeThis");
        doc.charset(Charset.forName(charsetUtf8));

        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlCharsetUTF8, doc.toString());

        Element selectedElement = doc.select("meta[charset]").first();
        assertEquals(charsetUtf8, doc.charset().name());
        assertEquals(charsetUtf8, selectedElement.attr("charset"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }

    @Test
    public void testMetaCharsetUpdateIso8859() {
        final Document doc = createHtmlDocument("changeThis");
        doc.charset(Charset.forName(charsetIso8859));

        final String htmlCharsetISO = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetIso8859 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlCharsetISO, doc.toString());

        Element selectedElement = doc.select("meta[charset]").first();
        assertEquals(charsetIso8859, doc.charset().name());
        assertEquals(charsetIso8859, selectedElement.attr("charset"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }

    @Test
    public void testMetaCharsetUpdateNoCharset() {
        final Document docNoCharset = Document.createShell("");
        docNoCharset.charset(Charset.forName(charsetUtf8));

        assertEquals(charsetUtf8, docNoCharset.select("meta[charset]").first().attr("charset"));

        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlCharsetUTF8, docNoCharset.toString());
    }

    @Test
    public void testMetaCharsetUpdateDisabled() {
        final Document docDisabled = Document.createShell("");

        final String htmlNoCharset = "<html>\n" +
                                        " <head></head>\n" +
                                        " <body></body>\n" +
                                        "</html>";
        assertEquals(htmlNoCharset, docDisabled.toString());
        assertNull(docDisabled.select("meta[charset]").first());
    }

    @Test
    public void testMetaCharsetUpdateDisabledNoChanges() {
        final Document doc = createHtmlDocument("dontTouch");

        final String htmlCharset = "<html>\n" +
                                    " <head>\n" +
                                    "  <meta charset=\"dontTouch\">\n" +
                                    "  <meta name=\"charset\" content=\"dontTouch\">\n" +
                                    " </head>\n" +
                                    " <body></body>\n" +
                                    "</html>";
        assertEquals(htmlCharset, doc.toString());

        Element selectedElement = doc.select("meta[charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("charset"));

        selectedElement = doc.select("meta[name=charset]").first();
        assertNotNull(selectedElement);
        assertEquals("dontTouch", selectedElement.attr("content"));
    }

    @Test
    public void testMetaCharsetUpdateEnabledAfterCharsetChange() {
        final Document doc = createHtmlDocument("dontTouch");
        doc.charset(Charset.forName(charsetUtf8));

        Element selectedElement = doc.select("meta[charset]").first();
        assertEquals(charsetUtf8, selectedElement.attr("charset"));
        assertTrue(doc.select("meta[name=charset]").isEmpty());
    }

    @Test
    public void testMetaCharsetUpdateCleanup() {
        final Document doc = createHtmlDocument("dontTouch");
        doc.charset(Charset.forName(charsetUtf8));

        final String htmlCharsetUTF8 = "<html>\n" +
                                        " <head>\n" +
                                        "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                                        " </head>\n" +
                                        " <body></body>\n" +
                                        "</html>";

        assertEquals(htmlCharsetUTF8, doc.toString());
    }

    @Test
    public void testMetaCharsetUpdateXmlUtf8() {
        final Document doc = createXmlDocument("1.0", "changeThis", true);
        doc.charset(Charset.forName(charsetUtf8));

        final String xmlCharsetUTF8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<root>node</root>";
        assertEquals(xmlCharsetUTF8, doc.toString());

        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals(charsetUtf8, doc.charset().name());
        assertEquals(charsetUtf8, selectedNode.attr("encoding"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }

    @Test
    public void testMetaCharsetUpdateXmlIso8859() {
        final Document doc = createXmlDocument("1.0", "changeThis", true);
        doc.charset(Charset.forName(charsetIso8859));

        final String xmlCharsetISO = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
            "<root>node</root>";
        assertEquals(xmlCharsetISO, doc.toString());

        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals(charsetIso8859, doc.charset().name());
        assertEquals(charsetIso8859, selectedNode.attr("encoding"));
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }

    @Test
    public void testMetaCharsetUpdateXmlNoCharset() {
        final Document doc = createXmlDocument("1.0", "none", false);
        doc.charset(Charset.forName(charsetUtf8));

        final String xmlCharsetUTF8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<root>node</root>";
        assertEquals(xmlCharsetUTF8, doc.toString());

        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals(charsetUtf8, selectedNode.attr("encoding"));
    }

    @Test
    public void testMetaCharsetUpdateXmlDisabled() {
        final Document doc = createXmlDocument("none", "none", false);

        final String xmlNoCharset = "<root>node</root>";
        assertEquals(xmlNoCharset, doc.toString());
    }

    @Test
    public void testMetaCharsetUpdateXmlDisabledNoChanges() {
        final Document doc = createXmlDocument("dontTouch", "dontTouch", true);

        final String xmlCharset = "<?xml version=\"dontTouch\" encoding=\"dontTouch\"?>\n" +
            "<root>node</root>";
        assertEquals(xmlCharset, doc.toString());

        XmlDeclaration selectedNode = (XmlDeclaration) doc.childNode(0);
        assertEquals("dontTouch", selectedNode.attr("encoding"));
        assertEquals("dontTouch", selectedNode.attr("version"));
    }

    private Document createHtmlDocument(String charset) {
        final Document doc = Document.createShell("");
        doc.head().appendElement("meta").attr("charset", charset);
        doc.head().appendElement("meta").attr("name", "charset").attr("content", charset);

        return doc;
    }

    private Document createXmlDocument(String version, String charset, boolean addDecl) {
        final Document doc = new Document("");
        doc.appendElement("root").text("node");
        doc.outputSettings().syntax(Syntax.xml);

        if(addDecl) {
            XmlDeclaration decl = new XmlDeclaration("xml", false);
            decl.attr("version", version);
            decl.attr("encoding", charset);
            doc.prependChild(decl);
        }

        return doc;
    }

    @Test void charsetOnEmptyDoc() {
        Document xml = new Document(Parser.NamespaceXml, "https://example.com"); // no nodes
        xml.outputSettings().syntax(Syntax.xml);
        xml.charset(StandardCharsets.UTF_8);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", xml.html());

        Document html = new Document("https://example.com");
        html.charset(StandardCharsets.UTF_8);
        assertEquals("<html><head><meta charset=\"UTF-8\"></head></html>", TextUtil.stripNewlines(html.html()));
    }

    @Test
    public void testShiftJisRoundtrip() throws Exception {
        String input =
                "<html>"
                        +   "<head>"
                        +     "<meta http-equiv=\"content-type\" content=\"text/html; charset=Shift_JIS\" />"
                        +   "</head>"
                        +   "<body>"
                        +     "before&nbsp;after"
                        +   "</body>"
                        + "</html>";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII));

        Document doc = Jsoup.parse(is, null, "http://example.com");
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

        String output = new String(doc.html().getBytes(doc.outputSettings().charset()), doc.outputSettings().charset());

        assertFalse(output.contains("?"), "Should not have contained a '?'.");
        assertTrue(output.contains("&#xa0;") || output.contains("&nbsp;"),
                "Should have contained a '&#xa0;' or a '&nbsp;'.");
    }

    @Test public void parseAndHtmlOnDifferentThreads() throws InterruptedException {
        String html = "<p>Alrighty then it's not \uD83D\uDCA9. <span>Next</span></p>"; // 💩
        String asci = "<p>Alrighty then it's not &#x1f4a9;. <span>Next</span></p>";

        final Document doc = Jsoup.parse(html);
        final String[] out = new String[1];
        final Elements p = doc.select("p");
        assertEquals(html, p.outerHtml());

        Thread thread = new Thread(() -> {
            out[0] = p.outerHtml();
            doc.outputSettings().charset(StandardCharsets.US_ASCII);
        });
        thread.start();
        thread.join();

        assertEquals(html, out[0]);
        assertEquals(StandardCharsets.US_ASCII, doc.outputSettings().charset());
        assertEquals(asci, p.outerHtml());
    }

    @Test public void testDocumentTypeGet() {
        String html = "\n\n<!-- comment -->  <!doctype html><p>One</p>";
        Document doc = Jsoup.parse(html);
        DocumentType documentType = doc.documentType();
        assertNotNull(documentType);
        assertEquals("html", documentType.name());
    }

    @Test public void framesetSupportsBodyMethod() {
        String html = "<html><head><title>Frame Test</title></head><frameset id=id><frame src=foo.html></frameset>";
        Document doc = Jsoup.parse(html);
        Element head = doc.head();
        assertNotNull(head);
        assertEquals("Frame Test", doc.title());

        // Frameset docs per html5 spec have no body element - but instead a frameset elelemt
        assertNull(doc.selectFirst("body"));
        Element frameset = doc.selectFirst("frameset");
        assertNotNull(frameset);

        // the body() method returns body or frameset and does not otherwise modify the document
        // doing it in body() vs parse keeps the html close to original for round-trip option
        Element body = doc.body();
        assertNotNull(body);
        assertSame(frameset, body);
        assertEquals("frame", body.child(0).tagName());

        assertNull(doc.selectFirst("body")); // did not vivify a body element

        String expected = "<html>\n" +
            " <head>\n" +
            "  <title>Frame Test</title>\n" +
            " </head>\n" +
            " <frameset id=\"id\">\n" +
            "  <frame src=\"foo.html\">\n" +
            " </frameset>\n" +
            "</html>";
        assertEquals(expected, doc.html());
    }

    @Test void forms() {
        String html = "<body><form id=1><input name=foo></form><form id=2><input name=bar>";
        Document doc = Jsoup.parse(html);

        List<FormElement> forms = doc.forms();
        assertEquals(2, forms.size());
        FormElement form = forms.get(1);
        assertEquals(1, form.elements().size());
        assertEquals("bar", form.elements().first().attr("name"));

        String emptyHtml = "<body>";
        Document emptyDoc = Jsoup.parse(emptyHtml);
        assertEquals(0, emptyDoc.forms().size());
    }

    @Test void expectForm() {
        String html = "<body><div name=form></div><form id=1 name=form><input name=foo></form><form id=2><input name=bar>";
        Document doc = Jsoup.parse(html);

        // test finds first <form>
        FormElement formEl1 = doc.expectForm("[name=form]");
        assertEquals("1", formEl1.id()); // and not the div

        FormElement formEl2 = doc.expectForm("form");
        assertEquals("1", formEl2.id());

        FormElement formEl3 = doc.expectForm("form:has([name=bar])");
        assertEquals("2", formEl3.id());

        boolean threw = false;
        try {
            FormElement nix = doc.expectForm("div");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);

    }
}
