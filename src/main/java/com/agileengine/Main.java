package com.agileengine;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.agileengine.finder.JsoupElementFinder.findElementById;
import static com.agileengine.finder.JsoupElementFinder.findElementsByQuery;
import static com.google.common.base.Preconditions.checkArgument;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final String DEFAULT_ELEMENT_ID = "make-everything-ok-button";

    // 0 original html file path
    // 1 new html file path
    // 2 element id
    public static void main(String[] args) {
        checkArgument(args.length >= 2, "expected at least file path to orignal html  file and file path to new html file");

        String filePath = args[0];
        String newFilePath = args[1];
        String elementId = args.length == 3
                ? args[2]
                : DEFAULT_ELEMENT_ID;

        Optional<Element> elementOptional = findElementById(new File(filePath), elementId);
        Optional<Element> parentElementOptional = elementOptional.map(Main::getParentDiv);

        if (!parentElementOptional.isPresent()) {
            LOGGER.info("element with id={} not found", elementId);
            return;
        }

        Element parentElement = parentElementOptional.get();
        Element element = elementOptional.get();

        if (parentElement.parent().children().size() > 1) {
            parentElement = parentElement.parent();
        }

        Optional<Elements> posibleContainersOptional = findElementsByQuery(new File(newFilePath), parentElement.cssSelector());

        if (!posibleContainersOptional.isPresent()) {
            LOGGER.info("Unable to find element matching={}", parentElement);
            return;
        }

        Elements posibleContainers = posibleContainersOptional.get();
        List<Element> elements = posibleContainers.stream()
                .flatMap(posibleContainer -> posibleContainer.children().stream())
                .flatMap(childrenElements -> childrenElements.children().stream())
                .collect(Collectors.toList());

        List<ElementMatchesPair> elementMatchesPairs = buildElementMatchesPairs(elements, element.attributes());

        String pathToElement = getPathToElement(getElementMatch(elementMatchesPairs), null);

        LOGGER.info("Path to element {}", pathToElement);
    }

    private static List<ElementMatchesPair> buildElementMatchesPairs(List<Element> elements, Attributes attributes) {
        List<ElementMatchesPair> elementMatchesPairs = new ArrayList<>();
        for (Element e : elements) {
            int attributesMatches = 0;
            for (Attribute attribute : attributes) {
                String attributeKey = attribute.getKey();

                if (e.hasAttr(attribute.getKey())
                        && e.attr(attributeKey).equals(attribute.getValue())) {
                    attributesMatches++;
                }
            }
            elementMatchesPairs.add(new ElementMatchesPair(e, attributesMatches));
        }

        return elementMatchesPairs;
    }

    private static Element getElementMatch(List<ElementMatchesPair> elementMatchesPairs) {
        int matches = 0;
        Element elementMatchUp = null;
        for (ElementMatchesPair emp : elementMatchesPairs) {
            if (emp.getMatches() > matches) {
                elementMatchUp = emp.getElement();
                matches = emp.matches;
            }
        }

        return elementMatchUp;
    }

    private static String getPathToElement(Element element, String path) {
        if (element == null) {
            return path;
        }
        if (path == null || path.isEmpty()) {
            return getPathToElement(element.parent(), element.tagName() + ":" + element.cssSelector());
        }

        return getPathToElement(element.parent(), element.tagName() + ":" + element.elementSiblingIndex() + " -> " + path);
    }

    private static Element getParentDiv(Element element) {
        Element parent = element.parent();
        if ("div".equals(parent.tagName())) {
            return parent;
        }

        return getParentDiv(parent);
    }

    // I could import//use a pair already made on a library but for this case I think this is fine
    private static class ElementMatchesPair {
        private Element element;
        private int matches;

        ElementMatchesPair(Element element, int matches) {
            this.element = element;
            this.matches = matches;
        }

        Element getElement() {
            return element;
        }

        int getMatches() {
            return matches;
        }
    }
}
