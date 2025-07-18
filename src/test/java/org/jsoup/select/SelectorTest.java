package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.jsoup.select.EvaluatorDebug.sexpr;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the selector selects correctly.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class SelectorTest {

    /** Test that the selected elements match exactly the specified IDs. */
    public static void assertSelectedIds(Elements els, String... ids) {
        assertNotNull(els);
        assertEquals(ids.length, els.size(), "Incorrect number of selected elements");
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], els.get(i).id(), "Incorrect content at index");
        }
    }

    public static void assertSelectedOwnText(Elements els, String... ownTexts) {
        assertNotNull(els);
        assertEquals(ownTexts.length, els.size(), "Incorrect number of selected elements");
        for (int i = 0; i < ownTexts.length; i++) {
            assertEquals(ownTexts[i], els.get(i).ownText(), "Incorrect content at index");
        }
    }

    @Test public void testByTag() {
        // should be case-insensitive
        Elements els = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><DIV id=3>").select("DIV");
        assertSelectedIds(els, "1", "2", "3");

        Elements none = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select("span");
        assertTrue(none.isEmpty());
    }

    @Test public void byEscapedTag() {
        // tested same result as js document.querySelector
        Document doc = Jsoup.parse("<p.p>One</p.p> <p\\p>Two</p\\p>");

        Element one = doc.expectFirst("p\\.p");
        assertEquals("One", one.text());

        Element two = doc.expectFirst("p\\\\p");
        assertEquals("Two", two.text());
    }

    @Test public void testById() {
        Elements els = Jsoup.parse("<div><p id=foo>Hello</p><p id=foo>Foo two!</p></div>").select("#foo");
        assertSelectedOwnText(els, "Hello", "Foo two!");

        Elements none = Jsoup.parse("<div id=1></div>").select("#foo");
        assertTrue(none.isEmpty());
    }

    @Test public void byEscapedId() {
        Document doc = Jsoup.parse("<p id='i.d'>One</p> <p id='i\\d'>Two</p> <p id='one-two/three'>Three</p>");

        Element one = doc.expectFirst("#i\\.d");
        assertEquals("One", one.text());

        Element two = doc.expectFirst("#i\\\\d");
        assertEquals("Two", two.text());

        Element thr = doc.expectFirst("p#one-two\\/three");
        assertEquals("Three", thr.text());
    }

    @Test public void testByClass() {
        Elements els = Jsoup.parse("<p id=0 class='ONE two'><p id=1 class='one'><p id=2 class='two'>").select("P.One");
        assertSelectedIds(els, "0", "1");

        Elements none = Jsoup.parse("<div class='one'></div>").select(".foo");
        assertTrue(none.isEmpty());

        Elements els2 = Jsoup.parse("<div class='One-Two' id=1></div>").select(".one-two");
        assertSelectedIds(els2, "1");
    }

    @Test public void byEscapedClass() {
        Document doc = Jsoup.parse("<p class='one.two#three'>One</p>");
        assertSelectedOwnText(doc.select("p.one\\.two\\#three"), "One");
    }

    @Test public void testByClassCaseInsensitive() {
        String html = "<p Class=foo>One <p Class=Foo>Two <p class=FOO>Three <p class=farp>Four";
        Elements elsFromClass = Jsoup.parse(html).select("P.Foo");
        Elements elsFromAttr = Jsoup.parse(html).select("p[class=foo]");

        assertEquals(elsFromAttr.size(), elsFromClass.size());
        assertSelectedOwnText(elsFromClass, "One", "Two", "Three");
    }


    @MultiLocaleTest
    public void testByAttribute(Locale locale) {
        Locale.setDefault(locale);

        String h = "<div Title=Foo /><div Title=Bar /><div Style=Qux /><div title=Balim /><div title=SLIM />" +
                "<div data-name='with spaces'/>";
        Document doc = Jsoup.parse(h);

        Elements withTitle = doc.select("[title]");
        assertEquals(4, withTitle.size());

        Elements foo = doc.select("[TITLE=foo]");
        assertEquals(1, foo.size());

        Elements foo2 = doc.select("[title=\"foo\"]");
        assertEquals(1, foo2.size());

        Elements foo3 = doc.select("[title=\"Foo\"]");
        assertEquals(1, foo3.size());

        Elements dataName = doc.select("[data-name=\"with spaces\"]");
        assertEquals(1, dataName.size());
        assertEquals("with spaces", dataName.first().attr("data-name"));

        Elements not = doc.select("div[title!=bar]");
        assertEquals(5, not.size());
        assertEquals("Foo", not.first().attr("title"));

        Elements starts = doc.select("[title^=ba]");
        assertEquals(2, starts.size());
        assertEquals("Bar", starts.first().attr("title"));
        assertEquals("Balim", starts.last().attr("title"));

        Elements ends = doc.select("[title$=im]");
        assertEquals(2, ends.size());
        assertEquals("Balim", ends.first().attr("title"));
        assertEquals("SLIM", ends.last().attr("title"));

        Elements contains = doc.select("[title*=i]");
        assertEquals(2, contains.size());
        assertEquals("Balim", contains.first().attr("title"));
        assertEquals("SLIM", contains.last().attr("title"));
    }

    @Test public void testNamespacedTag() {
        Document doc = Jsoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>");
        Elements byTag = doc.select("abc|def");
        assertSelectedIds(byTag, "1", "2");

        Elements byAttr = doc.select(".bold");
        assertSelectedIds(byAttr, "2");

        Elements byTagAttr = doc.select("abc|def.bold");
        assertSelectedIds(byTagAttr, "2");

        Elements byContains = doc.select("abc|def:contains(e)");
        assertSelectedIds(byContains, "1", "2");
    }

    @Test public void testWildcardNamespacedTag() {
        Document doc = Jsoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>");
        Elements byTag = doc.select("*|def");
        assertSelectedIds(byTag, "1", "2");

        Elements byAttr = doc.select(".bold");
        assertSelectedIds(byAttr, "2");

        Elements byTagAttr = doc.select("*|def.bold");
        assertSelectedIds(byTagAttr, "2");

        Elements byContains = doc.select("*|def:contains(e)");
        assertSelectedIds(byContains, "1", "2");
    }

    @Test public void testNamespacedWildcardTag() {
        // https://github.com/jhy/jsoup/issues/1811
        Document doc = Jsoup.parse("<p>One</p> <ac:p id=2>Two</ac:p> <ac:img id=3>Three</ac:img>");
        Elements byNs = doc.select("ac|*");
        assertSelectedIds(byNs, "2", "3");
    }

    @Test public void testWildcardNamespacedXmlTag() {
        Document doc = Jsoup.parse(
            "<div><Abc:Def id=1>Hello</Abc:Def></div> <Abc:Def class=bold id=2>There</abc:def>",
            "", Parser.xmlParser()
        );

        Elements byTag = doc.select("*|Def");
        assertSelectedIds(byTag, "1", "2");

        Elements byAttr = doc.select(".bold");
        assertSelectedIds(byAttr, "2");

        Elements byTagAttr = doc.select("*|Def.bold");
        assertSelectedIds(byTagAttr, "2");

        Elements byContains = doc.select("*|Def:contains(e)");
        assertSelectedIds(byContains, "1", "2");
    }

    @Test public void testWildCardNamespacedCaseVariations() {
        Document doc = Jsoup.parse("<One:Two>One</One:Two><three:four>Two</three:four>", "", Parser.xmlParser());
        Elements els1 = doc.select("One|Two");
        Elements els2 = doc.select("one|two");
        Elements els3 = doc.select("Three|Four");
        Elements els4 = doc.select("three|Four");

        assertEquals(els1, els2);
        assertEquals(els3, els4);
        assertEquals("One", els1.text());
        assertEquals(1, els1.size());
        assertEquals("Two", els3.text());
        assertEquals(1, els2.size());
    }

    @MultiLocaleTest
    public void testByAttributeStarting(Locale locale) {
        Locale.setDefault(locale);

        Document doc = Jsoup.parse("<div id=1 ATTRIBUTE data-name=jsoup>Hello</div><p data-val=5 id=2>There</p><p id=3>No</p>");
        Elements withData = doc.select("[^data-]");
        assertEquals(2, withData.size());
        assertEquals("1", withData.first().id());
        assertEquals("2", withData.last().id());

        withData = doc.select("p[^data-]");
        assertEquals(1, withData.size());
        assertEquals("2", withData.first().id());

        assertEquals(1, doc.select("[^attrib]").size());
    }

    @Test public void testByAttributeRegex() {
        Document doc = Jsoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif><img></p>");
        Elements imgs = doc.select("img[src~=(?i)\\.(png|jpe?g)]");
        assertSelectedIds(imgs, "1", "2", "3");
    }

    @Test public void testByAttributeRegexCharacterClass() {
        Document doc = Jsoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif id=4></p>");
        Elements imgs = doc.select("img[src~=[o]]");
        assertSelectedIds(imgs, "1", "4");
    }

    @Test public void testByAttributeRegexCombined() {
        Document doc = Jsoup.parse("<div><table class=x><td>Hello</td></table></div>");
        Elements els = doc.select("div table[class~=x|y]");
        assertEquals(1, els.size());
        assertEquals("Hello", els.text());
    }

    @Test public void testCombinedWithContains() {
        Document doc = Jsoup.parse("<p id=1>One</p><p>Two +</p><p>Three +</p>");
        Elements els = doc.select("p#1 + :contains(+)");
        assertEquals(1, els.size());
        assertEquals("Two +", els.text());
        assertEquals("p", els.first().tagName());
    }

    @Test public void testAllElements() {
        String h = "<div><p>Hello</p><p><b>there</b></p></div>";
        Document doc = Jsoup.parse(h);
        Elements allDoc = doc.select("*");
        Elements allUnderDiv = doc.select("div *");
        assertEquals(8, allDoc.size());
        assertEquals(3, allUnderDiv.size());
        assertEquals("p", allUnderDiv.first().tagName());
    }

    @Test public void testAllWithClass() {
        String h = "<p class=first>One<p class=first>Two<p>Three";
        Document doc = Jsoup.parse(h);
        Elements ps = doc.select("*.first");
        assertEquals(2, ps.size());
    }

    @Test public void testGroupOr() {
        String h = "<div title=foo /><div title=bar /><div /><p></p><img /><span title=qux>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("p,div,[title]");

        assertEquals(5, els.size());
        assertEquals("div", els.get(0).tagName());
        assertEquals("foo", els.get(0).attr("title"));
        assertEquals("div", els.get(1).tagName());
        assertEquals("bar", els.get(1).attr("title"));
        assertEquals("div", els.get(2).tagName());
        assertEquals(0, els.get(2).attr("title").length()); // missing attributes come back as empty string
        assertFalse(els.get(2).hasAttr("title"));
        assertEquals("p", els.get(3).tagName());
        assertEquals("span", els.get(4).tagName());
    }

    @Test public void testGroupOrAttribute() {
        String h = "<div id=1 /><div id=2 /><div title=foo /><div title=bar />";
        Elements els = Jsoup.parse(h).select("[id],[title=foo]");

        assertEquals(3, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals("foo", els.get(2).attr("title"));
    }

    @Test public void descendant() {
        String h = "<div class=head><p class=first>Hello</p><p>There</p></div><p>None</p>";
        Document doc = Jsoup.parse(h);
        Element root = doc.getElementsByClass("HEAD").first();

        Elements els = root.select(".head p");
        assertEquals(2, els.size());
        assertEquals("Hello", els.get(0).text());
        assertEquals("There", els.get(1).text());

        Elements p = root.select("p.first");
        assertEquals(1, p.size());
        assertEquals("Hello", p.get(0).text());

        Elements empty = root.select("p .first"); // self, not descend, should not match
        assertEquals(0, empty.size());

        Elements aboveRoot = root.select("body div.head");
        assertEquals(0, aboveRoot.size());
    }

    @Test public void and() {
        String h = "<div id=1 class='foo bar' title=bar name=qux><p class=foo title=bar>Hello</p></div";
        Document doc = Jsoup.parse(h);

        Elements div = doc.select("div.foo");
        assertEquals(1, div.size());
        assertEquals("div", div.first().tagName());

        Elements p = doc.select("div .foo"); // space indicates like "div *.foo"
        assertEquals(1, p.size());
        assertEquals("p", p.first().tagName());

        Elements div2 = doc.select("div#1.foo.bar[title=bar][name=qux]"); // very specific!
        assertEquals(1, div2.size());
        assertEquals("div", div2.first().tagName());

        Elements p2 = doc.select("div *.foo"); // space indicates like "div *.foo"
        assertEquals(1, p2.size());
        assertEquals("p", p2.first().tagName());
    }

    @Test public void deeperDescendant() {
        String h = "<div class=head><p><span class=first>Hello</div><div class=head><p class=first><span>Another</span><p>Again</div>";
        Document doc = Jsoup.parse(h);
        Element root = doc.getElementsByClass("head").first();

        Elements els = root.select("div p .first");
        assertEquals(1, els.size());
        assertEquals("Hello", els.first().text());
        assertEquals("span", els.first().tagName());

        Elements aboveRoot = root.select("body p .first");
        assertEquals(0, aboveRoot.size());
    }

    @Test public void parentChildElement() {
        String h = "<div id=1><div id=2><div id = 3></div></div></div><div id=4></div>";
        Document doc = Jsoup.parse(h);

        Elements divs = doc.select("div > div");
        assertEquals(2, divs.size());
        assertEquals("2", divs.get(0).id()); // 2 is child of 1
        assertEquals("3", divs.get(1).id()); // 3 is child of 2

        Elements div2 = doc.select("div#1 > div");
        assertEquals(1, div2.size());
        assertEquals("2", div2.get(0).id());
    }

    @Test public void parentWithClassChild() {
        String h = "<h1 class=foo><a href=1 /></h1><h1 class=foo><a href=2 class=bar /></h1><h1><a href=3 /></h1>";
        Document doc = Jsoup.parse(h);

        Elements allAs = doc.select("h1 > a");
        assertEquals(3, allAs.size());
        assertEquals("a", allAs.first().tagName());

        Elements fooAs = doc.select("h1.foo > a");
        assertEquals(2, fooAs.size());
        assertEquals("a", fooAs.first().tagName());

        Elements barAs = doc.select("h1.foo > a.bar");
        assertEquals(1, barAs.size());
    }

    @Test public void parentChildStar() {
        String h = "<div id=1><p>Hello<p><b>there</b></p></div><div id=2><span>Hi</span></div>";
        Document doc = Jsoup.parse(h);
        Elements divChilds = doc.select("div > *");
        assertEquals(3, divChilds.size());
        assertEquals("p", divChilds.get(0).tagName());
        assertEquals("p", divChilds.get(1).tagName());
        assertEquals("span", divChilds.get(2).tagName());
    }

    @Test public void streamParentChildStar() {
        String h = "<div id=1><p>Hello<p><b>there</b></p></div><div id=2><span>Hi</span></div>";
        Document doc = Jsoup.parse(h);

        List<Element> divChilds = doc.selectStream("div > *")
            .collect(Collectors.toList());

        assertEquals(3, divChilds.size());
        assertEquals("p", divChilds.get(0).tagName());
        assertEquals("p", divChilds.get(1).tagName());
        assertEquals("span", divChilds.get(2).tagName());
    }

    @Test public void multiChildDescent() {
        String h = "<div id=foo><h1 class=bar><a href=http://example.com/>One</a></h1></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("div#foo > h1.bar > a[href*=example]");
        assertEquals(1, els.size());
        assertEquals("a", els.first().tagName());
    }

    @Test public void caseInsensitive() {
        String h = "<dIv tItle=bAr><div>"; // mixed case so a simple toLowerCase() on value doesn't catch
        Document doc = Jsoup.parse(h);

        assertEquals(2, doc.select("DiV").size());
        assertEquals(1, doc.select("DiV[TiTLE]").size());
        assertEquals(1, doc.select("DiV[TiTLE=BAR]").size());
        assertEquals(0, doc.select("DiV[TiTLE=BARBARELLA]").size());
    }

    @Test public void adjacentSiblings() {
        String h = "<ol><li>One<li>Two<li>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("li + li");
        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }

    @Test public void adjacentSiblingsWithId() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("li#1 + li#2");
        assertEquals(1, sibs.size());
        assertEquals("Two", sibs.get(0).text());
    }

    @Test public void notAdjacent() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("li#1 + li#3");
        assertEquals(0, sibs.size());
    }

    @Test public void mixCombinator() {
        String h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("body > div.foo li + li");

        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }

    @Test public void mixCombinatorGroup() {
        String h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select(".foo > ol, ol > li + li");

        assertEquals(3, els.size());
        assertEquals("ol", els.get(0).tagName());
        assertEquals("Two", els.get(1).text());
        assertEquals("Three", els.get(2).text());
    }

    @Test public void generalSiblings() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("#1 ~ #3");
        assertEquals(1, els.size());
        assertEquals("Three", els.first().text());
    }

    @Test public void elelemtDescendantSkipsNodes() {
        String h = "<div class=foo> <!-- foo --> <ol> <li>One<li><!-- bar --> Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select(".foo > ol, ol > li + li");

        assertEquals(3, els.size());
        assertEquals("ol", els.get(0).tagName());
        assertEquals("Two", els.get(1).text());
        assertEquals("Three", els.get(2).text());
    }

    @Test public void siblingsSkipNodes() {
        String h = "<ol><li id=1><!-- foo -->One<li id=2><!-- foo -->Two<li id=3><!-- foo -->Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("#1 ~ #3");
        assertEquals(1, els.size());
        assertEquals("Three", els.first().text());
    }

    // for http://github.com/jhy/jsoup/issues#issue/10
    @Test public void testCharactersInIdAndClass() {
        // using CSS spec for identifiers (id and class): a-z0-9, -, _. NOT . (which is OK in html spec, but not css)
        String h = "<div><p id='a1-foo_bar'>One</p><p class='b2-qux_bif'>Two</p></div>";
        Document doc = Jsoup.parse(h);

        Element el1 = doc.getElementById("a1-foo_bar");
        assertEquals("One", el1.text());
        Element el2 = doc.getElementsByClass("b2-qux_bif").first();
        assertEquals("Two", el2.text());

        Element el3 = doc.select("#a1-foo_bar").first();
        assertEquals("One", el3.text());
        Element el4 = doc.select(".b2-qux_bif").first();
        assertEquals("Two", el4.text());
    }

    // for http://github.com/jhy/jsoup/issues#issue/13
    @Test public void testSupportsLeadingCombinator() {
        String h = "<div><p><span>One</span><span>Two</span></p></div>";
        Document doc = Jsoup.parse(h);

        Element p = doc.select("div > p").first();
        Elements spans = p.select("> span");
        assertEquals(2, spans.size());
        assertEquals("One", spans.first().text());

        // make sure doesn't get nested
        h = "<div id=1><div id=2><div id=3></div></div></div>";
        doc = Jsoup.parse(h);
        Element div = doc.select("div").select(" > div").first();
        assertEquals("2", div.id());
    }

    @Test public void testPseudoLessThan() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:lt(2)");
        assertEquals(3, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Two", ps.get(1).text());
        assertEquals("Four", ps.get(2).text());
    }

    @Test public void testPseudoGreaterThan() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:gt(0)");
        assertEquals(2, ps.size());
        assertEquals("Two", ps.get(0).text());
        assertEquals("Three", ps.get(1).text());
    }

    @Test public void testPseudoEquals() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:eq(0)");
        assertEquals(2, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Four", ps.get(1).text());

        Elements ps2 = doc.select("div:eq(0) p:eq(0)");
        assertEquals(1, ps2.size());
        assertEquals("One", ps2.get(0).text());
        assertEquals("p", ps2.get(0).tagName());
    }

    @Test public void testPseudoBetween() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:gt(0):lt(2)");
        assertEquals(1, ps.size());
        assertEquals("Two", ps.get(0).text());
    }

    @Test public void testPseudoCombined() {
        Document doc = Jsoup.parse("<div class='foo'><p>One</p><p>Two</p></div><div><p>Three</p><p>Four</p></div>");
        Elements ps = doc.select("div.foo p:gt(0)");
        assertEquals(1, ps.size());
        assertEquals("Two", ps.get(0).text());
    }

    @Test public void testPseudoHas() {
        Document doc = Jsoup.parse("<div id=0><p><span>Hello</span></p></div> <div id=1><span class=foo>There</span></div> <div id=2><p>Not</p></div>");

        Elements divs1 = doc.select("div:has(span)");
        assertEquals(2, divs1.size());
        assertEquals("0", divs1.get(0).id());
        assertEquals("1", divs1.get(1).id());

        Elements divs2 = doc.select("div:has([class])");
        assertEquals(1, divs2.size());
        assertEquals("1", divs2.get(0).id());

        Elements divs3 = doc.select("div:has(span, p)");
        assertEquals(3, divs3.size());
        assertEquals("0", divs3.get(0).id());
        assertEquals("1", divs3.get(1).id());
        assertEquals("2", divs3.get(2).id());

        Elements els1 = doc.body().select(":has(p)");
        assertEquals(3, els1.size()); // body, div, div
        assertEquals("body", els1.first().tagName());
        assertEquals("0", els1.get(1).id());
        assertEquals("2", els1.get(2).id());

        Elements els2 = doc.body().select(":has(> span)");
        assertEquals(2,els2.size()); // p, div
        assertEquals("p",els2.first().tagName());
        assertEquals("1", els2.get(1).id());
    }

    @Test public void testNestedHas() {
        Document doc = Jsoup.parse("<div><p><span>One</span></p></div> <div><p>Two</p></div>");
        Elements divs = doc.select("div:has(p:has(span))");
        assertEquals(1, divs.size());
        assertEquals("One", divs.first().text());

        // test matches in has
        divs = doc.select("div:has(p:matches((?i)two))");
        assertEquals(1, divs.size());
        assertEquals("div", divs.first().tagName());
        assertEquals("Two", divs.first().text());

        // test contains in has
        divs = doc.select("div:has(p:contains(two))");
        assertEquals(1, divs.size());
        assertEquals("div", divs.first().tagName());
        assertEquals("Two", divs.first().text());
    }

    @Test public void testHasSibling() {
        // https://github.com/jhy/jsoup/issues/2137
        Document doc = Jsoup.parse("<h1 id=1>One</h1> <h2>Two</h2> <h1>Three</h1>");
        Elements els = doc.select("h1:has(+h2)");
        assertSelectedIds(els, "1");

        els = doc.select("h1:has(~h1)");
        assertSelectedIds(els, "1");

        // nested with sibling
        doc = Jsoup.parse("<div id=1><p><i>One</i><i>Two</p><p><i>Three</p></div> <div><p><i>Four</div>");
        els = doc.select("div:has(p:has(i:has(~i)))");
        assertSelectedIds(els, "1");
    }

    @MultiLocaleTest
    public void testPseudoContains(Locale locale) {
        Locale.setDefault(locale);

        Document doc = Jsoup.parse("<div><p>The Rain.</p> <p class=light>The <i>RAIN</i>.</p> <p>Rain, the.</p></div>");

        Elements ps1 = doc.select("p:contains(Rain)");
        assertEquals(3, ps1.size());

        Elements ps2 = doc.select("p:contains(the rain)");
        assertEquals(2, ps2.size());
        assertEquals("The Rain.", ps2.first().html());
        assertEquals("The <i>RAIN</i>.", ps2.last().html());

        Elements ps3 = doc.select("p:contains(the Rain):has(i)");
        assertEquals(1, ps3.size());
        assertEquals("light", ps3.first().className());

        Elements ps4 = doc.select(".light:contains(rain)");
        assertEquals(1, ps4.size());
        assertEquals("light", ps3.first().className());

        Elements ps5 = doc.select(":contains(rain)");
        assertEquals(8, ps5.size()); // html, body, div,...

        Elements ps6 = doc.select(":contains(RAIN)");
        assertEquals(8, ps6.size());
    }

    @Test public void testPsuedoContainsWithParentheses() {
        Document doc = Jsoup.parse("<div><p id=1>This (is good)</p><p id=2>This is bad)</p>");

        Elements ps1 = doc.select("p:contains(this (is good))");
        assertEquals(1, ps1.size());
        assertEquals("1", ps1.first().id());

        Elements ps2 = doc.select("p:contains(this is bad\\))");
        assertEquals(1, ps2.size());
        assertEquals("2", ps2.first().id());
    }

    @Test void containsWholeText() {
        Document doc = Jsoup.parse("<div><p> jsoup\n The <i>HTML</i> Parser</p><p>jsoup The HTML Parser</div>");
        Elements ps = doc.select("p");

        Elements es1 = doc.select("p:containsWholeText( jsoup\n The HTML Parser)");
        Elements es2 = doc.select("p:containsWholeText(jsoup The HTML Parser)");
        assertEquals(1, es1.size());
        assertEquals(1, es2.size());
        assertEquals(ps.get(0), es1.first());
        assertEquals(ps.get(1), es2.first());

        assertEquals(0, doc.select("div:containsWholeText(jsoup the html parser)").size());
        assertEquals(0, doc.select("div:containsWholeText(jsoup\n the html parser)").size());

        doc = Jsoup.parse("<div><p></p><p> </p><p>.  </p>");
        Elements blanks = doc.select("p:containsWholeText(  )");
        assertEquals(1, blanks.size());
        assertEquals(".  ", blanks.first().wholeText());
    }

    @Test void containsWholeOwnText() {
        Document doc = Jsoup.parse("<div><p> jsoup\n The <i>HTML</i> Parser</p><p>jsoup The HTML Parser<br></div>");
        Elements ps = doc.select("p");

        Elements es1 = doc.select("p:containsWholeOwnText( jsoup\n The  Parser)");
        Elements es2 = doc.select("p:containsWholeOwnText(jsoup The HTML Parser\n)");
        assertEquals(1, es1.size());
        assertEquals(1, es2.size());
        assertEquals(ps.get(0), es1.first());
        assertEquals(ps.get(1), es2.first());

        assertEquals(0, doc.select("div:containsWholeOwnText(jsoup the html parser)").size());
        assertEquals(0, doc.select("div:containsWholeOwnText(jsoup\n the  parser)").size());

        doc = Jsoup.parse("<div><p></p><p> </p><p>.  </p>");
        Elements blanks = doc.select("p:containsWholeOwnText(  )");
        assertEquals(1, blanks.size());
        assertEquals(".  ", blanks.first().wholeText());
    }

    @MultiLocaleTest
    public void containsOwn(Locale locale) {
        Locale.setDefault(locale);

        Document doc = Jsoup.parse("<p id=1>Hello <b>there</b> igor</p>");
        Elements ps = doc.select("p:containsOwn(Hello IGOR)");
        assertEquals(1, ps.size());
        assertEquals("1", ps.first().id());

        assertEquals(0, doc.select("p:containsOwn(there)").size());

        Document doc2 = Jsoup.parse("<p>Hello <b>there</b> IGOR</p>");
        assertEquals(1, doc2.select("p:containsOwn(igor)").size());

    }

    @Test public void testMatches() {
        Document doc = Jsoup.parse("<p id=1>The <i>Rain</i></p> <p id=2>There are 99 bottles.</p> <p id=3>Harder (this)</p> <p id=4>Rain</p>");

        Elements p1 = doc.select("p:matches(The rain)"); // no match, case sensitive
        assertEquals(0, p1.size());

        Elements p2 = doc.select("p:matches((?i)the rain)"); // case insense. should include root, html, body
        assertEquals(1, p2.size());
        assertEquals("1", p2.first().id());

        Elements p4 = doc.select("p:matches((?i)^rain$)"); // bounding
        assertEquals(1, p4.size());
        assertEquals("4", p4.first().id());

        Elements p5 = doc.select("p:matches(\\d+)");
        assertEquals(1, p5.size());
        assertEquals("2", p5.first().id());

        Elements p6 = doc.select("p:matches(\\w+\\s+\\(\\w+\\))"); // test bracket matching
        assertEquals(1, p6.size());
        assertEquals("3", p6.first().id());

        Elements p7 = doc.select("p:matches((?i)the):has(i)"); // multi
        assertEquals(1, p7.size());
        assertEquals("1", p7.first().id());
    }

    @Test public void matchesOwn() {
        Document doc = Jsoup.parse("<p id=1>Hello <b>there</b> now</p>");

        Elements p1 = doc.select("p:matchesOwn((?i)hello now)");
        assertEquals(1, p1.size());
        assertEquals("1", p1.first().id());

        assertEquals(0, doc.select("p:matchesOwn(there)").size());
    }

    @Test public void matchesWholeText() {
        Document doc = Jsoup.parse("<p id=1>Hello <b>there</b>\n now</p><p id=2> </p><p id=3></p>");

        Elements p1 = doc.select("p:matchesWholeText((?i)hello there\n now)");
        assertEquals(1, p1.size());
        assertEquals("1", p1.first().id());

        assertEquals(1, doc.select("p:matchesWholeText(there\n now)").size());
        assertEquals(0, doc.select("p:matchesWholeText(There\n now)").size());

        Elements p2 = doc.select("p:matchesWholeText(^\\s+$)");
        assertEquals(1, p2.size());
        assertEquals("2", p2.first().id());

        Elements p3 = doc.select("p:matchesWholeText(^$)");
        assertEquals(1, p3.size());
        assertEquals("3", p3.first().id());
    }

    @Test public void matchesWholeOwnText() {
        Document doc = Jsoup.parse("<p id=1>Hello <b>there</b>\n now</p><p id=2> </p><p id=3><i>Text</i></p>");

        Elements p1 = doc.select("p:matchesWholeOwnText((?i)hello \n now)");
        assertEquals(1, p1.size());
        assertEquals("1", p1.first().id());

        assertEquals(0, doc.select("p:matchesWholeOwnText(there\n now)").size());

        Elements p2 = doc.select("p:matchesWholeOwnText(^\\s+$)");
        assertEquals(1, p2.size());
        assertEquals("2", p2.first().id());

        Elements p3 = doc.select("p:matchesWholeOwnText(^$)");
        assertEquals(1, p3.size());
        assertEquals("3", p3.first().id());
    }

    @Test public void testRelaxedTags() {
        Document doc = Jsoup.parse("<abc_def id=1>Hello</abc_def> <abc-def id=2>There</abc-def>");

        Elements el1 = doc.select("abc_def");
        assertEquals(1, el1.size());
        assertEquals("1", el1.first().id());

        Elements el2 = doc.select("abc-def");
        assertEquals(1, el2.size());
        assertEquals("2", el2.first().id());
    }

    @Test public void notParas() {
        Document doc = Jsoup.parse("<p id=1>One</p> <p>Two</p> <p><span>Three</span></p>");

        Elements el1 = doc.select("p:not([id=1])");
        assertEquals(2, el1.size());
        assertEquals("Two", el1.first().text());
        assertEquals("Three", el1.last().text());

        Elements el2 = doc.select("p:not(:has(span))");
        assertEquals(2, el2.size());
        assertEquals("One", el2.first().text());
        assertEquals("Two", el2.last().text());
    }

    @Test public void notAll() {
        Document doc = Jsoup.parse("<p>Two</p> <p><span>Three</span></p>");

        Elements el1 = doc.body().select(":not(p)"); // should just be the span
        assertEquals(2, el1.size());
        assertEquals("body", el1.first().tagName());
        assertEquals("span", el1.last().tagName());
    }

    @Test public void notClass() {
        Document doc = Jsoup.parse("<div class=left>One</div><div class=right id=1><p>Two</p></div>");

        Elements el1 = doc.select("div:not(.left)");
        assertEquals(1, el1.size());
        assertEquals("1", el1.first().id());
    }

    @Test public void handlesCommasInSelector() {
        Document doc = Jsoup.parse("<p name='1,2'>One</p><div>Two</div><ol><li>123</li><li>Text</li></ol>");

        Elements ps = doc.select("[name=1,2]");
        assertEquals(1, ps.size());

        Elements containers = doc.select("div, li:matches([0-9,]+)");
        assertEquals(2, containers.size());
        assertEquals("div", containers.get(0).tagName());
        assertEquals("li", containers.get(1).tagName());
        assertEquals("123", containers.get(1).text());
    }

    @Test public void selectSupplementaryCharacter() {
        String s = new String(Character.toChars(135361));
        Document doc = Jsoup.parse("<div k" + s + "='" + s + "'>^" + s +"$/div>");
        assertEquals("div", doc.select("div[k" + s + "]").first().tagName());
        assertEquals("div", doc.select("div:containsOwn(" + s + ")").first().tagName());
    }

    @Test
    public void selectClassWithSpace() {
        final String html = "<div class=\"value\">class without space</div>\n"
                          + "<div class=\"value \">class with space</div>";

        Document doc = Jsoup.parse(html);

        Elements found = doc.select("div[class=value ]");
        assertEquals(2, found.size());
        assertEquals("class without space", found.get(0).text());
        assertEquals("class with space", found.get(1).text());

        found = doc.select("div[class=\"value \"]");
        assertEquals(2, found.size());
        assertEquals("class without space", found.get(0).text());
        assertEquals("class with space", found.get(1).text());

        found = doc.select("div[class=\"value\\ \"]");
        assertEquals(0, found.size());
    }

    @Test public void selectSameElements() {
        final String html = "<div>one</div><div>one</div>";

        Document doc = Jsoup.parse(html);
        Elements els = doc.select("div");
        assertEquals(2, els.size());

        Elements subSelect = els.select(":contains(one)");
        assertEquals(2, subSelect.size());
    }

    @Test public void attributeWithBrackets() {
        String html = "<div data='End]'>One</div> <div data='[Another)]]'>Two</div>";
        Document doc = Jsoup.parse(html);
        assertEquals("One", doc.select("div[data='End]']").first().text());
        assertEquals("Two", doc.select("div[data='[Another)]]']").first().text());
        assertEquals("One", doc.select("div[data=\"End]\"]").first().text());
        assertEquals("Two", doc.select("div[data=\"[Another)]]\"]").first().text());
    }

    @MultiLocaleTest
    public void containsData(Locale locale) {
        Locale.setDefault(locale);

        String html = "<p>function</p><script>FUNCTION</script><style>item</style><span><!-- comments --></span>";
        Document doc = Jsoup.parse(html);
        Element body = doc.body();

        Elements dataEls1 = body.select(":containsData(function)");
        Elements dataEls2 = body.select("script:containsData(function)");
        Elements dataEls3 = body.select("span:containsData(comments)");
        Elements dataEls4 = body.select(":containsData(o)");
        Elements dataEls5 = body.select("style:containsData(ITEM)");

        assertEquals(2, dataEls1.size()); // body and script
        assertEquals(1, dataEls2.size());
        assertEquals(dataEls1.last(), dataEls2.first());
        assertEquals("<script>FUNCTION</script>", dataEls2.outerHtml());
        assertEquals(1, dataEls3.size());
        assertEquals("span", dataEls3.first().tagName());
        assertEquals(3, dataEls4.size());
        assertEquals("body", dataEls4.first().tagName());
        assertEquals("script", dataEls4.get(1).tagName());
        assertEquals("span", dataEls4.get(2).tagName());
        assertEquals(1, dataEls5.size());
    }

    @Test public void containsWithQuote() {
        String html = "<p>One'One</p><p>One'Two</p>";
        Document doc = Jsoup.parse(html);
        Elements els = doc.select("p:contains(One\\'One)");
        assertEquals(1, els.size());
        assertEquals("One'One", els.text());
    }

    @Test public void selectFirst() {
        String html = "<p>One<p>Two<p>Three";
        Document doc = Jsoup.parse(html);
        assertEquals("One", doc.selectFirst("p").text());
    }

    @Test public void selectFirstWithAnd() {
        String html = "<p>One<p class=foo>Two<p>Three";
        Document doc = Jsoup.parse(html);
        assertEquals("Two", doc.selectFirst("p.foo").text());
    }

    @Test public void selectFirstWithOr() {
        String html = "<p>One<p>Two<p>Three<div>Four";
        Document doc = Jsoup.parse(html);
        assertEquals("One", doc.selectFirst("p, div").text());
    }

    @Test public void matchText() {
        String html = "<p>One<br>Two</p>";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        String origHtml = doc.html();

        Elements one = doc.select("p:matchText:first-child");
        assertEquals("One", one.first().text());

        Elements two = doc.select("p:matchText:last-child");
        assertEquals("Two", two.first().text());

        assertEquals(origHtml, doc.html());

        assertEquals("Two", doc.select("p:matchText + br + *").text());
    }

    @Test public void nthLastChildWithNoParent() {
        Element el = new Element("p").text("Orphan");
        Elements els = el.select("p:nth-last-child(1)");
        assertEquals(0, els.size());
    }

    @Test public void splitOnBr() {
        String html = "<div><p>One<br>Two<br>Three</p></div>";
        Document doc = Jsoup.parse(html);

        Elements els = doc.select("p:matchText");
        assertEquals(3, els.size());
        assertEquals("One", els.get(0).text());
        assertEquals("Two", els.get(1).text());
        assertEquals("Three", els.get(2).toString());
    }

    @Test public void matchTextAttributes() {
        Document doc = Jsoup.parse("<div><p class=one>One<br>Two<p class=two>Three<br>Four");
        Elements els = doc.select("p.two:matchText:last-child");

        assertEquals(1, els.size());
        assertEquals("Four", els.text());
    }

    @Test public void findBetweenSpan() {
        Document doc = Jsoup.parse("<p><span>One</span> Two <span>Three</span>");
        Elements els = doc.select("span ~ p:matchText"); // the Two becomes its own p, sibling of the span
        // todo - think this should really be 'p:matchText span ~ p'. The :matchText should behave as a modifier to expand the nodes.

        assertEquals(1, els.size());
        assertEquals("Two", els.text());
    }

    @Test public void startsWithBeginsWithSpace() {
        Document doc = Jsoup.parse("<small><a href=\" mailto:abc@def.net\">(abc@def.net)</a></small>");
        Elements els = doc.select("a[href^=' mailto']");

        assertEquals(1, els.size());
    }

    @Test public void endsWithEndsWithSpaces() {
        Document doc = Jsoup.parse("<small><a href=\" mailto:abc@def.net \">(abc@def.net)</a></small>");
        Elements els = doc.select("a[href$='.net ']");

        assertEquals(1, els.size());
    }

    // https://github.com/jhy/jsoup/issues/1257
    private final String mixedCase =
        "<html xmlns:n=\"urn:ns\"><n:mixedCase>text</n:mixedCase></html>";
    private final String lowercase =
        "<html xmlns:n=\"urn:ns\"><n:lowercase>text</n:lowercase></html>";

    @Test
    public void html_mixed_case_simple_name() {
        Document doc = Jsoup.parse(mixedCase, "", Parser.htmlParser());
        assertEquals(0, doc.select("mixedCase").size());
    }

    @Test
    public void html_mixed_case_wildcard_name() {
        Document doc = Jsoup.parse(mixedCase, "", Parser.htmlParser());
        assertEquals(1, doc.select("*|mixedCase").size());
    }

    @Test
    public void html_lowercase_simple_name() {
        Document doc = Jsoup.parse(lowercase, "", Parser.htmlParser());
        assertEquals(0, doc.select("lowercase").size());
    }

    @Test
    public void html_lowercase_wildcard_name() {
        Document doc = Jsoup.parse(lowercase, "", Parser.htmlParser());
        assertEquals(1, doc.select("*|lowercase").size());
    }

    @Test
    public void xml_mixed_case_simple_name() {
        Document doc = Jsoup.parse(mixedCase, "", Parser.xmlParser());
        assertEquals(0, doc.select("mixedCase").size());
    }

    @Test
    public void xml_mixed_case_wildcard_name() {
        Document doc = Jsoup.parse(mixedCase, "", Parser.xmlParser());
        assertEquals(1, doc.select("*|mixedCase").size());
    }

    @Test
    public void xml_lowercase_simple_name() {
        Document doc = Jsoup.parse(lowercase, "", Parser.xmlParser());
        assertEquals(0, doc.select("lowercase").size());
    }

    @Test
    public void xml_lowercase_wildcard_name() {
        Document doc = Jsoup.parse(lowercase, "", Parser.xmlParser());
        assertEquals(1, doc.select("*|lowercase").size());
    }

    @Test
    public void trimSelector() {
        // https://github.com/jhy/jsoup/issues/1274
        Document doc = Jsoup.parse("<p><span>Hello");
        Elements els = doc.select(" p span ");
        assertEquals(1, els.size());
        assertEquals("Hello", els.first().text());
    }

    @Test
    public void xmlWildcardNamespaceTest() {
        // https://github.com/jhy/jsoup/issues/1208
        Document doc = Jsoup.parse("<ns1:MyXmlTag>1111</ns1:MyXmlTag><ns2:MyXmlTag>2222</ns2:MyXmlTag>", "", Parser.xmlParser());
        Elements select = doc.select("*|MyXmlTag");
        assertEquals(2, select.size());
        assertEquals("1111", select.get(0).text());
        assertEquals("2222", select.get(1).text());
    }

    @Test
    public void childElements() {
        // https://github.com/jhy/jsoup/issues/1292
        String html = "<body><span id=1>One <span id=2>Two</span></span></body>";
        Document doc = Jsoup.parse(html);

        Element outer = doc.selectFirst("span");
        Element span = outer.selectFirst("span");
        Element inner = outer.selectFirst("* span");

        assertEquals("1", outer.id());
        assertEquals("1", span.id());
        assertEquals("2", inner.id());
        assertEquals(outer, span);
        assertNotEquals(outer, inner);
    }

    @Test
    public void selectFirstLevelChildrenOnly() {
        // testcase for https://github.com/jhy/jsoup/issues/984
        String html = "<div><span>One <span>Two</span></span> <span>Three <span>Four</span></span>";
        Document doc = Jsoup.parse(html);

        Element div = doc.selectFirst("div");
        assertNotNull(div);

        // want to select One and Three only - the first level children
        Elements spans = div.select(":root > span");
        assertEquals(2, spans.size());
        assertEquals("One Two", spans.get(0).text());
        assertEquals("Three Four", spans.get(1).text());
    }

    @Test
    public void wildcardNamespaceMatchesNoNamespace() {
        // https://github.com/jhy/jsoup/issues/1565
        String xml = "<package><meta>One</meta><opf:meta>Two</opf:meta></package>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());

        Elements metaEls = doc.select("meta");
        assertEquals(1, metaEls.size());
        assertEquals("One", metaEls.get(0).text());

        Elements nsEls = doc.select("*|meta");
        assertEquals(2, nsEls.size());
        assertEquals("One", nsEls.get(0).text());
        assertEquals("Two", nsEls.get(1).text());
    }

    @Test void containsTextQueryIsNormalized() {
        Document doc = Jsoup.parse("<p><p id=1>Hello  there now<em>!</em>");
        Elements a = doc.select("p:contains(Hello   there  now!)");
        Elements b = doc.select(":containsOwn(hello   there  now)");
        Elements c = doc.select("p:contains(Hello there now)");
        Elements d = doc.select(":containsOwn(hello There now)");
        Elements e = doc.select("p:contains(HelloThereNow)");

        assertEquals(1, a.size());
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a, d);
        assertEquals(0, e.size());
        assertNotEquals(a, e);
    }

    @Test public void selectorExceptionNotStringFormatException() {
        Selector.SelectorParseException ex = new Selector.SelectorParseException("%&");
        assertEquals("%&", ex.getMessage());
    }

    @Test public void evaluatorMemosAreReset() {
        Evaluator eval = QueryParser.parse("p ~ p");
        CombiningEvaluator.And andEval = (CombiningEvaluator.And) eval;
        StructuralEvaluator.PreviousSibling prevEval = (StructuralEvaluator.PreviousSibling) andEval.evaluators.get(0);
        IdentityHashMap<Node, IdentityHashMap<Node, Boolean>> map = prevEval.threadMemo.get();
        assertEquals(0, map.size()); // no memo yet

        Document doc1 = Jsoup.parse("<p>One<p>Two<p>Three");
        Document doc2 = Jsoup.parse("<p>One2<p>Two2<p>Three2");

        Elements s1 = doc1.select(eval);
        assertEquals(2, s1.size());
        assertEquals("Two", s1.first().text());
        Elements s2 = doc2.select(eval);
        assertEquals(2, s2.size());
        assertEquals("Two2", s2.first().text());

        assertEquals(1, map.size()); // root of doc 2
    }

    @Test public void blankTextNodesAreConsideredEmpty() {
        // https://github.com/jhy/jsoup/issues/1976
        String html = "<li id=1>\n </li><li id=2></li><li id=3> </li><li id=4>One</li><li id=5><span></li>";
        Document doc = Jsoup.parse(html);
        Elements empty = doc.select("li:empty");
        Elements notEmpty = doc.select("li:not(:empty)");

        assertSelectedIds(empty, "1", "2", "3");
        assertSelectedIds(notEmpty, "4", "5");
    }

    @Test
    public void emptyPseudo() {
        // https://github.com/jhy/jsoup/issues/2130
        String html = "<ul>" +
            "  <li id='1'>\n </li>" + // Blank text node only
            "  <li id='2'></li>" + // No nodes
            "  <li id='3'><!-- foo --></li>" + // Comment node only
            "  <li id='4'>One</li>" + // Text node with content
            "  <li id='5'><span></span></li>" + // Element node
            "  <li id='6'>\n <span></span></li>" + // Blank text node followed by an element
            "  <li id='7'><!-- foo --><i></i></li>" + // Comment node with element
            "</ul>";
        Document doc = Jsoup.parse(html);
        Elements empty = doc.select("li:empty");
        assertSelectedIds(empty, "1", "2", "3");

        Elements notEmpty = doc.select("li:not(:empty)");
        assertSelectedIds(notEmpty, "4", "5", "6", "7");
    }

    @Test public void parentFromSpecifiedDescender() {
        // https://github.com/jhy/jsoup/issues/2018
        String html = "<ul id=outer><li>Foo</li><li>Bar <ul id=inner><li>Baz</li><li>Qux</li></ul> </li></ul>";
        Document doc = Jsoup.parse(html);

        Element ul = doc.expectFirst("#outer");
        assertEquals(2, ul.childrenSize());

        Element li1 = ul.expectFirst("> li:nth-child(1)");
        assertEquals("Foo", li1.ownText());
        assertTrue(li1.select("ul").isEmpty());

        Element li2 = ul.expectFirst("> li:nth-child(2)");
        assertEquals("Bar", li2.ownText());

        // And now for the bug - li2 select was not restricted to the li2 context
        Elements innerLis = li2.select("ul > li");
        assertSelectedOwnText(innerLis, "Baz", "Qux");

        // Confirm that parent selector (" ") works same as immediate parent (">");
        Elements innerLisFromParent = li2.select("ul li");
        assertEquals(innerLis, innerLisFromParent);
    }

    @Test public void rootImmediateParentSubquery() {
        // a combinator at the start of the query is applied to the Root selector. i.e. "> p" matches a P immediately parented
        // by the Root (which is <html> for a top level query, or the context element in :has)
        // in the sub query, the combinator was dropped incorrectly
        String html = "<p id=0><span>A</p> <p id=1><b><i><span>B</p> <p id=2><i>C</p>\n";
        Document doc = Jsoup.parse(html);

        Elements els = doc.select("p:has(> span, > i)"); // should match a p with an immediate span or i
        assertSelectedIds(els, "0", "2");
    }

    @Test public void is() {
        String html = "<h1 id=1><p></p></h1> <section><h1 id=2></h1></section> <article><h2 id=3></h2></article> <h2 id=4><p></p></h2>";
        Document doc = Jsoup.parse(html);

        assertSelectedIds(
            doc.select(":is(section, article) :is(h1, h2, h3)"),
            "2", "3");

        assertSelectedIds(
            doc.select(":is(section, article) ~ :is(h1, h2, h3):has(p)"),
            "4");

        assertSelectedIds(
            doc.select(":is(h1:has(p), h2:has(section), h3)"),
            "1");

        assertSelectedIds(
            doc.select(":is(h1, h2, h3):has(p)"),
            "1", "4");

        String query = "div :is(h1, h2)";
        Evaluator parse = QueryParser.parse(query);
        assertEquals(query, parse.toString());
    }

    @Test public void orAfterClass() {
        // see also QueryParserTest#parsesOrAfterAttribute
        // https://github.com/jhy/jsoup/issues/2073
        Document doc = Jsoup.parse("<div id=parent><span class=child></span><span class=child></span><span class=child></span></div>");
        String q = "#parent [class*=child], .some-other-selector .nested";
        assertEquals("(Or (And (AttributeWithValueContaining '[class*=child]')(Ancestor (Id '#parent')))(And (Class '.nested')(Ancestor (Class '.some-other-selector'))))", sexpr(q));
        Elements els = doc.select(q);
        assertEquals(3, els.size());
    }

    @Test public void emptyAttributePrefix() {
        // https://github.com/jhy/jsoup/issues/2079
        // Discovered feature: [^] should find elements with any attribute (any prefix)
        String html = "<p one>One<p one two>Two<p>Three";
        Document doc = Jsoup.parse(html);

        Elements els = doc.select("[^]");
        assertSelectedOwnText(els, "One", "Two");

        Elements emptyAttr = doc.select("p:not([^])");
        assertSelectedOwnText(emptyAttr, "Three");
    }

    @Test public void anyAttribute() {
        // https://github.com/jhy/jsoup/issues/2079
        String html = "<div id=1><p one>One<p one two>Two<p>Three";
        Document doc = Jsoup.parse(html);

        Elements els = doc.select("p[*]");
        assertSelectedOwnText(els, "One", "Two");

        Elements emptyAttr = doc.select("p:not([*])");
        assertSelectedOwnText(emptyAttr, "Three");
    }

    @Test void divHasSpanPreceding() {
        // https://github.com/jhy/jsoup/issues/2187
        String html = "<div><span>abc</span><a>def</a></div>";
        String q = "div:has(span + a)";

        Document doc = Jsoup.parse(html);
        Elements els = doc.select(q);
        assertEquals(1, els.size());
        assertEquals("div", els.first().normalName());
    }

    @Test void divHasDivPreceding() {
        // https://github.com/jhy/jsoup/issues/2131
        String html = "<div id=1>\n" +
            "<div 1><span>hello</span></div>\n" +
            "<div 2><span>there</span></div>\n" +
            "\n" +
            "</div>";

        String q = "div:has(>div + div)";

        Document doc = Jsoup.parse(html);
        Elements els = doc.select(q);
        assertEquals(1, els.size());
        assertEquals("div", els.first().normalName());
        assertEquals("1", els.first().id());
    }

    @Test void nestedMultiHas() {
        // https://github.com/jhy/jsoup/issues/2131
        String html =
            "<html>" +
                "<head></head>" +
                "<body>" +
                "<div id=o>" +
                "<div id=i1><span id=s1>hello</span></div>" +
                "<div id=i2><span id=s2>world</span></div>" +
                "</div>" +
                "</body></html>";
        Document document = Jsoup.parse(html);

        String q = "div:has(> div:has(> span) + div:has(> span))";
        Elements els = document.select(q);
        assertEquals(1, els.size());
        assertEquals("o", els.get(0).id());
    }

    @Test void negativeNthChild() {
        // https://github.com/jhy/jsoup/issues/1147
        String html = "<p>1</p> <p>2</p> <p>3</p> <p>4</p>";
        Document doc = Jsoup.parse(html);

        // Digitless
        Elements pos = doc.select("p:nth-child(n+2)");
        assertSelectedOwnText(pos, "2", "3", "4");

        Elements neg = doc.select("p:nth-child(-n+2)");
        assertSelectedOwnText(neg, "1", "2");

        Elements combo = doc.select("p:nth-child(n+2):nth-child(-n+2)");
        assertSelectedOwnText(combo, "2");

        // Digitful, 2n+2 or -1n+2
        Elements pos2 = doc.select("p:nth-child(2n+2)");
        assertSelectedOwnText(pos2, "2", "4");

        Elements neg2 = doc.select("p:nth-child(-1n+2)");
        assertSelectedOwnText(neg2, "1", "2");
    }

    // Tests that nested structural and combining evaluators get reset
    private static class ResetTracker extends Evaluator {
        boolean resetCalled = false;
        @Override
        public boolean matches(Element root, Element element) {
            return true;
        }

        @Override
        protected void reset() {
            resetCalled = true;
            super.reset();
        }
    }

    @Test void notResetCascades() {
        ResetTracker track = new ResetTracker();
        StructuralEvaluator.Not structEval = new StructuralEvaluator.Not(track);

        Document doc = Jsoup.parse("<div><p>Test</p></div>");
        Element p = doc.expectFirst("p");
        structEval.matches(doc, p);

        assertFalse(structEval.threadMemo.get().isEmpty());
        assertFalse(track.resetCalled);

        structEval.reset();
        assertTrue(structEval.threadMemo.get().isEmpty());
        assertTrue(track.resetCalled);
    }

    @Test void testImmediateParentRunCascades() {
        ResetTracker child = new ResetTracker();
        ResetTracker parent = new ResetTracker();

        StructuralEvaluator.ImmediateParentRun run = new StructuralEvaluator.ImmediateParentRun(child);
        run.add(parent);

        Document doc = Jsoup.parse("<div><p><span>Test</span></p></div>");
        Element span = doc.expectFirst("span");
        assertTrue(run.matches(doc, span));

        run.reset();
        assertTrue(child.resetCalled);
        assertTrue(parent.resetCalled);
    }

    @Test
    public void testAncestorChain() {
        ResetTracker grandParent = new ResetTracker();
        ResetTracker parent = new ResetTracker();
        ResetTracker child = new ResetTracker();

        StructuralEvaluator.Ancestor b_needs_a = new StructuralEvaluator.Ancestor(grandParent);
        StructuralEvaluator.Ancestor c_needs_b = new StructuralEvaluator.Ancestor(parent);
        CombiningEvaluator.And chain = new CombiningEvaluator.And(child, c_needs_b, b_needs_a);

        Document doc = Jsoup.parse("<div class='A'><p class='B'><span class='C'>Test</span></p></div>");
        Element span = doc.expectFirst("span");
        assertTrue(chain.matches(doc, span), "Should match span in correct ancestor chain");

        chain.reset();
        assertTrue(grandParent.resetCalled);
        assertTrue(parent.resetCalled);
        assertTrue(child.resetCalled);
        assertTrue(b_needs_a.threadMemo.get().isEmpty());
        assertTrue(c_needs_b.threadMemo.get().isEmpty());
    }

    @Test void hexDigitUnescape() {
        // tests the select component of https://github.com/jhy/jsoup/pull/2297, with per-spec escapes
        // literal is: #\30 \%\ Platform\ Image
        String html = "<img id='0% Platform Image'>";
        String q = "#\\30 \\%\\ Platform\\ Image";

        Document doc = Jsoup.parse(html);
        Element img = doc.expectFirst(q);
        assertEquals("img", img.tagName());
    }

    @Test void escapeCssIdentifier() {
        // thorough tests are in TokenQueue
        assertEquals("-\\30 a", Selector.escapeCssIdentifier("-0a"));
        assertEquals("a0b", Selector.escapeCssIdentifier("a0b"));
    }

    @Test void unescapeCssIdentifier() {
        // thorough tests are in TokenQueue
        assertEquals("-0a", Selector.unescapeCssIdentifier("-\\30 a"));
        assertEquals("a0b", Selector.unescapeCssIdentifier("a0b"));
    }

    @Test void evaluatorOf() {
        Evaluator eval = Selector.evaluatorOf("div > p");
        assertEquals("div > p", eval.toString());
    }

    @Test void hasComment() {
        Document doc = Jsoup.parse("<div id=1>One</div><div id=2>Two <!-- foo --></div>");
        Elements els = doc.select("div:has(::comment)");
        assertSelectedIds(els, "2");
    }

    @Test void hasCommentWithText() {
        Document doc = Jsoup.parse("<div id=1>One <!-- qux bar --></div><div id=2>Two <!-- foo qux --></div>");
        Elements els = doc.select("div:has(::comment:contains(foo):contains(qux))");
        assertSelectedIds(els, "2");
    }

    @Test void descendantComment() {
        Document doc = Jsoup.parse("<div id=1><div id=2><!-- comment2 --><div id=3><!-- comment3 --></div></div></div><div id=4><div id=5><div id=6>Not</div></div></div>");

        String q = "div > div:has(::comment)";
        assertEquals("(ImmediateParentRun (Tag 'div')(And (Tag 'div')(Has (InstanceType '::comment'))))", sexpr(q));
        Elements els1 = doc.select(q);
        assertSelectedIds(els1, "2", "3");

        String q2 = "div div:has(>::comment:contains(comment3))";
        assertEquals("(And (Ancestor (Tag 'div'))(And (Tag 'div')(Has (ImmediateParentRun (Root '>')(And (InstanceType '::comment')(ContainsValue ':contains(comment3)'))))))", sexpr(q2));
        Elements els2 = doc.select(q2);
        assertSelectedIds(els2, "3");

        String q3 = "div:has(>::comment) div";
        assertEquals("(And (Tag 'div')(Ancestor (And (Tag 'div')(Has (ImmediateParentRun (Root '>')(InstanceType '::comment'))))))", sexpr(q3));
        Elements els3 = doc.select(q3);
        assertSelectedIds(els3, "3");
    }

    @Test void nodeWithElementAncestor() {
        Document doc = Jsoup.parse("<div id=1><div id=2><p> <!-- comment --></p></div></div>");
        String q = "div:has(p ::comment)";
        assertEquals("(And (Tag 'div')(Has (And (InstanceType '::comment')(Ancestor (Tag 'p')))))", sexpr(q));
        Elements els = doc.select(q);
        assertSelectedIds(els, "1", "2");
    }

    @Test void precedingComment() {
        Document doc = Jsoup.parse("<div><!-- comment --><p id=1><p id=2></div><div><p id=3><p id=4>");

        String q = "::comment ~ p";
        assertEquals("(And (Tag 'p')(PreviousSibling (InstanceType '::comment')))", sexpr(q));
        Elements els1 = doc.select(q);
        assertSelectedIds(els1, "1", "2");

        String q2 = "::comment + p";
        assertEquals("(And (Tag 'p')(ImmediatePreviousSibling (InstanceType '::comment')))", sexpr(q2));
        Elements els2 = doc.select(q2);
        assertSelectedIds(els2, "1");
    }

    @Test void datanode() {
        Document doc = Jsoup.parse("<div id=1> <!-- foo --> </div> <div id=2> <script>foo</script> </div> <div><script>bar></script>");
        String q = "div:has(::data:contains(foo))";
        assertEquals("(And (Tag 'div')(Has (And (InstanceType '::data')(ContainsValue ':contains(foo)'))))", sexpr(q));
        Elements els = doc.select(q);
        assertSelectedIds(els, "2");
    }

    @Test void leafNode() {
        Document doc = Jsoup.parse("<div id=1></div><div id=2> </div>");
        String q = "div:has(::leafnode)";
        assertEquals("(And (Tag 'div')(Has (InstanceType '::leafnode')))", sexpr(q));
        Elements els = doc.select(q);
        assertSelectedIds(els, "2");
    }

    @Test void leafNodeContains() {
        Document doc = Jsoup.parse("<div id=1>foo</div><div id=2><!-- bar --></div><div id=3>Bar</div><div id=4><script id=5> Bar </script></div>");
        String q = "div:has(::leafnode:contains(Bar))";
        assertEquals("(And (Tag 'div')(Has (And (InstanceType '::leafnode')(ContainsValue ':contains(bar)'))))", sexpr(q));
        Elements els = doc.select(q);
        assertSelectedIds(els, "2", "3", "4");
    }

    @Test void nodeContains() {
        Document doc = Jsoup.parse("<div><p>One</p></div><div>Two</div>");
        String q = "div ::node:contains(One)";
        Nodes<Node> nodes = doc.selectNodes(Selector.evaluatorOf(q));
        // should have the P and the Text (because nodeValue is ownText)
        assertEquals(2, nodes.size());
        assertEquals("One", nodes.get(0).nodeValue());
        assertEquals("p", nodes.get(0).nodeName());
        assertEquals("One", nodes.get(1).nodeValue());
        assertEquals("#text", nodes.get(1).nodeName());
    }

    @Test void selectComment() {
        Document doc = Jsoup.parse("<div><!-- find this --></div><!-- and this --><p><!-- not that --></p>");
        String q = "::comment:contains(this)";
        assertEquals("(And (InstanceType '::comment')(ContainsValue ':contains(this)'))", sexpr(q));
        Nodes<Comment> comments = doc.selectNodes(q, Comment.class);

        assertEquals(2, comments.size());
        assertEquals(" find this ", comments.get(0).getData());
        assertEquals(" find this ", comments.get(0).nodeValue());
        assertEquals(" and this ", comments.get(1).getData());

        Nodes<Node> nodes = doc.selectNodes("::comment");
        assertEquals(3, nodes.size());
        assertEquals(" find this ", nodes.get(0).nodeValue());
        assertEquals(" and this ", nodes.get(1).nodeValue());
        assertEquals(" not that ", nodes.get(2).nodeValue());
    }

    @Test void selectTextNodes() {
        Document doc = Jsoup.parse("<p>One</p> <p>Two</p>");
        Nodes<TextNode> text = doc.selectNodes("p ::text", TextNode.class);
        assertEquals(2, text.size());
        assertEquals("One", text.get(0).getWholeText());
        assertEquals("Two", text.get(1).getWholeText());
    }

    @Test void elementsViaNodeInterface() {
        Document doc = Jsoup.parse("<p>One</p> <p>Two</p>");
        Nodes<Element> ps = doc.selectNodes("p", Element.class);
        assertEquals(2, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Two", ps.get(1).text());
    }

    @Test void blankNodes() {
        Document doc = Jsoup.parse("<p> </p><p><!--  --><!----></p><p>\n</p><p>One</p><p><!-- two --></p>");

        Nodes<Node> nodes = doc.selectNodes("::node:blank", Node.class);
        assertEquals(12, nodes.size());
        assertEquals("#document", nodes.get(0).nodeName());
        assertEquals("html", nodes.get(1).nodeName());
        assertEquals("head", nodes.get(2).nodeName());
        assertEquals("body", nodes.get(3).nodeName());
        assertEquals("p", nodes.get(4).nodeName());
        assertEquals("#text", nodes.get(5).nodeName());
        assertEquals("p", nodes.get(6).nodeName());
        assertEquals("#comment", nodes.get(7).nodeName());
        assertEquals("#comment", nodes.get(8).nodeName());
        assertEquals("p", nodes.get(9).nodeName());
        assertEquals("#text", nodes.get(10).nodeName());
        assertEquals("p", nodes.get(11).nodeName());

        Nodes<Comment> comments = doc.selectNodes("::comment:blank", Comment.class);
        assertEquals(2, comments.size());
        assertEquals("  ", comments.get(0).getData());
        assertEquals("", comments.get(1).getData());

        String notBlank = "::comment:not(:blank)";
        Evaluator notBlankEval = QueryParser.parse(notBlank);
        assertEquals("(And (InstanceType '::comment')(Not (BlankValue ':blank')))", sexpr(notBlankEval));
        Nodes<Comment> commentsWithData = doc.selectNodes(notBlankEval, Comment.class);
        assertEquals(1, commentsWithData.size());
        assertEquals(" two ", commentsWithData.get(0).getData());
    }

    @Test void blankElements() {
        Document doc = Jsoup.parse("<p id=1>  </p><p id=2>One</p><p id=3><span>One</span></p>");
        Elements els = doc.select("p:blank");
        assertSelectedIds(els, "1", "3");
    }

    @Test void nonBlankText() {
        Document doc = Jsoup.parse("<p id=1>  </p><p id=2>One</p><p id=3><span id=4>Two</span></p>");
        Elements els = doc.select(":not(:blank)");
        assertSelectedIds(els, "2", "4");

        Nodes<TextNode> text = doc.selectNodes("::text:not(:blank)", TextNode.class);
        assertEquals(2, text.size());
        assertEquals("One", text.get(0).getWholeText());
        assertEquals("Two", text.get(1).getWholeText());
    }

    @Test void nodeMatches() {
        Document doc = Jsoup.parse("<p>1234</p> <p>123</p> <p>12</p> <p>1</p> <!--4321--> <!--432--> <!-- 43 -->");

        String regex = "::leafnode:matches(\\d{3,4})";
        Evaluator eval = Selector.evaluatorOf(regex);
        assertEquals("(And (InstanceType '::leafnode')(MatchesValue ':matches(\\d{3,4})'))", sexpr(eval));

        Nodes<Node> nodes = doc.selectNodes(eval);
        assertEquals(4, nodes.size());
        assertEquals("1234", nodes.get(0).nodeValue());
        assertEquals("123", nodes.get(1).nodeValue());
        assertEquals("4321", nodes.get(2).nodeValue());
        assertEquals("432", nodes.get(3).nodeValue());
    }

    @Test void cdataNodes() {
        String xml = "<body><![CDATA[One]]><p>Two</p><![CDATA[Three]]><x><![CDATA[ ]]></body>";
        Document doc = Jsoup.parse(xml, Parser.xmlParser());

        // via leafnode:
        Nodes<CDataNode> leafnodes = doc.selectNodes("::leafnode", CDataNode.class);
        assertEquals(3, leafnodes.size());

        // cdata via unfiltered
        Nodes<Node> nodes = doc.selectNodes("::cdata");
        assertEquals(3, nodes.size());

        // (not) blank:
        Nodes<CDataNode> notBlanks = doc.selectNodes("::cdata:not(:blank)", CDataNode.class);
        assertEquals(2, notBlanks.size());
        assertEquals("One", notBlanks.get(0).nodeValue());
        assertEquals("Three", notBlanks.get(1).nodeValue());

        // contains:
        Nodes<CDataNode> contains = doc.selectNodes("::cdata:contains(One)", CDataNode.class);
        assertEquals(1, contains.size());
        assertEquals("One", contains.get(0).nodeValue());

        // matches:
        Nodes<CDataNode> matches = doc.selectNodes("::cdata:matches(re)", CDataNode.class);
        assertEquals(1, matches.size());
        assertEquals("Three", matches.get(0).nodeValue());
    }

    @Test void unknownPseudoNodeSelectError() {
        Exception ex = assertThrows(
            Selector.SelectorParseException.class,
            () -> Selector.evaluatorOf("::unknown:contains(foo)")
        );
        assertEquals(
            "Could not parse query '::unknown:contains(foo)': unknown node type '::unknown'",
            ex.getMessage()
        );
    }

}
