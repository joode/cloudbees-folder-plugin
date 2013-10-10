/*
 * The MIT License
 *
 * Copyright 2013 CloudBees.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cloudbees.hudson.plugins.folder.properties;

import com.cloudbees.hudson.plugins.folder.AbstractFolderTest;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import org.acegisecurity.AccessDeniedException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.concurrent.Callable;

public class AuthorizationMatrixPropertyTest extends AbstractFolderTest {
    public void testBasics1() throws Exception {
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);
        realm.createAccount("alice","alice");
        realm.createAccount("bob","bob");
        hudson.setSecurityRealm(realm);

        ProjectMatrixAuthorizationStrategy as = new ProjectMatrixAuthorizationStrategy();
        hudson.setAuthorizationStrategy(as);
        as.add(Hudson.READ,"authenticated");

        Folder f = createFolder();
        AuthorizationMatrixProperty amp = new AuthorizationMatrixProperty();
        amp.add(Item.READ,"alice");
        amp.add(Item.BUILD,"alice");
        f.getProperties().add(amp);

        final FreeStyleProject foo = f.createProject(FreeStyleProject.class, "foo");

        WebClient wc = createWebClient().login("bob");
        try {
            wc.getPage(foo);
            fail();
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(404, e.getStatusCode());
        }

        wc = createWebClient().login("alice");
        wc.getPage(foo);    // this should succeed

        // and build permission should be set, too
        wc.executeOnServer(new Callable<Object>() {
            public Object call() throws Exception {
                foo.checkPermission(Item.BUILD);
                try {
                    foo.checkPermission(Item.DELETE);
                    fail("acecss should be denied");
                } catch (AccessDeniedException e) {
                    // expected
                }
                return null;
            }
        });
    }
}
