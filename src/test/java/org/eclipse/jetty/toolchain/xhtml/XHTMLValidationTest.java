//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.toolchain.xhtml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.catalog.Catalog;

import org.eclipse.jetty.toolchain.test.MavenPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XHTMLValidationTest
{
    @Test
    public void testXhtmlValidation() throws IOException, InvalidXHTMLException
    {
        Path exampleHtml = MavenPaths.findTestResourceFile("example.xhtml");
        String xhtml = new String(Files.readAllBytes(exampleHtml), StandardCharsets.UTF_8);
        XHTMLValidator.validate(xhtml);
    }

    public static Stream<String> publicIds() throws IOException
    {
        Set<String> publicIds = new HashSet<>();
        Path catalogDir = MavenPaths.findMainResourceDir("org/eclipse/jetty/toolchain/xhtml");

        Pattern publicIdPattern = Pattern.compile("\"(-//[^\"]*)\"");

        List<IOException> errors = new ArrayList<>();

        try (Stream<Path> files = Files.list(catalogDir))
        {
            files.filter(Files::isRegularFile)
                .forEach((file) ->
                {
                    try
                    {
                        Files.readAllLines(file)
                            .forEach(line -> {
                                Matcher matcher = publicIdPattern.matcher(line);
                                if(matcher.find())
                                {
                                    publicIds.add(matcher.group(1));
                                }
                            });
                    }
                    catch (IOException e)
                    {
                        errors.add(e);
                    }
                });
        }

        if (!errors.isEmpty())
        {
            IOException oops = new IOException("Read failures");
            errors.forEach(oops::addSuppressed);
            throw oops;
        }

        // known public ids that we don't want to validate (as they are outside of the scope of this schema)
        publicIds.remove("-//OASIS//DTD Entity Resolution XML Catalog V1.0//EN");
        publicIds.remove("-//W3C//NOTATION AFDR ARCBASE XHTML 1.1//EN");
        publicIds.remove("-//Your Name Here//DTD XHTML Legacy 1.1//EN");

        // Remove all "-//W3C//NOTATION " references.
        List<String> notationRefs = publicIds.stream().filter(ref -> ref.startsWith("-//W3C//NOTATION ")).collect(Collectors.toList());
        for (String notationRef: notationRefs)
        {
            publicIds.remove(notationRef);
        }

        return publicIds.stream().sorted();
    }

    @ParameterizedTest
    @MethodSource("publicIds")
    public void testPublicIdResolving(String publicId)
    {
        // System.err.printf("testPublicIdResolving(\"%s\")%n", publicId);
        Catalog catalog = CatalogXHTML.getCatalogStrict();
        String resolved = catalog.matchPublic(publicId);
        assertNotNull(resolved);
    }

    public static Stream<String> systemIds() throws IOException
    {
        Set<String> systemIds = new HashSet<>();
        Path catalogDir = MavenPaths.findMainResourceDir("org/eclipse/jetty/toolchain/xhtml");

        Pattern systemIdPattern = Pattern.compile("\"(http://(www\\.)?w3\\.org/[^\"]*)\"");

        List<IOException> errors = new ArrayList<>();

        try (Stream<Path> files = Files.list(catalogDir))
        {
            files.filter(Files::isRegularFile)
                .forEach((file) ->
                {
                    try
                    {
                        Files.readAllLines(file)
                            .forEach(line -> {
                                Matcher matcher = systemIdPattern.matcher(line);
                                if(matcher.find())
                                {
                                    systemIds.add(matcher.group(1));
                                }
                            });
                    }
                    catch (IOException e)
                    {
                        errors.add(e);
                    }
                });
        }

        if (!errors.isEmpty())
        {
            IOException oops = new IOException("Read failures");
            errors.forEach(oops::addSuppressed);
            throw oops;
        }

        // known systems ids that we don't want to validate (as they are outside of the scope of this schema)
        systemIds.remove("http://www.w3.org/2001/XMLSchema-instance");

        return systemIds.stream().sorted();
    }

    @ParameterizedTest
    @MethodSource("systemIds")
    public void testSystemIdResolving(String systemId)
    {
        // System.err.printf("testSystemIdResolving(\"%s\")%n", systemId);
        Catalog catalog = CatalogXHTML.getCatalogStrict();
        String resolved = catalog.matchSystem(systemId);
        assertNotNull(resolved);
    }
}
