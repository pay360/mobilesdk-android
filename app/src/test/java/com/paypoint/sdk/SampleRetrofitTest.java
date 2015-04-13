package com.paypoint.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Who:  Pete
 * When: 13/04/2015
 * What:
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest="./app/src/main/AndroidManifest.xml", emulateSdk = 18, reportSdk = 18)
public class SampleRetrofitTest {
    private static final String API_URL = "https://api.github.com";

    static class Contributor {
        String login;
        int contributions;
    }

    interface GitHub {
        @GET("/repos/square/retrofit/contributors")
        List<Contributor> contributors(

        );
    }

    @Test
    public void testClient() {

        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        Robolectric.getFakeHttpLayer().interceptResponseContent(false);
        // Create a very simple REST adapter which points the GitHub API endpoint.
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .build();

        // Create an instance of our GitHub API interface.
        GitHub github = restAdapter.create(GitHub.class);

        // Fetch and print a list of the contributors to this library.
        List<Contributor> contributors = github.contributors();
        for (Contributor contributor : contributors) {
            System.out.println(contributor.login + " (" + contributor.contributions + ")");
        }
    }
}
