package com.vaibhav.tutorial.gdrive.sbt.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

@Controller
public class HomePageController {
	
	private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static com.google.api.client.json.JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	
	private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
	
	private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";
	
	@Value("${google.oauth.callback.url}")
	private String CALLBACK_URI;
	
	@Value("${google.secret.key.path}")
	private Resource gdSecretKey;
	
	@Value("${google.credentials.folder.path}")
	private Resource credentialsFolder;
	
	private GoogleAuthorizationCodeFlow flow;	//This variable will be initialize only once when application has started.

	@PostConstruct
	public void init() throws Exception{
			GoogleClientSecrets secret = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretKey.getInputStream()));
			flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,JSON_FACTORY,secret,SCOPES)
					.setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile())).build();	
	}
	
	@GetMapping(value = "/")
	public String homePage() throws IOException {
		boolean isUserAuthenticated = false;
		Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
		if(credential != null) {
			boolean tokenValid = credential.refreshToken();
			if(tokenValid) {
				isUserAuthenticated = true;
			}
		}
		
		return isUserAuthenticated ? "dashboard.html" : "index.html";
	}
	
	@GetMapping(value = {"/googlesignin"})
	public void doGoogleSignin(HttpServletResponse response) throws IOException {
		GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		String actualRedirecrURL = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();
		response.sendRedirect(actualRedirecrURL);
	}
	
	@GetMapping(value = "/oauth")
	public String saveAuthorisationCode(HttpServletRequest request) throws IOException {
		String code = request.getParameter("code");
				if(code != null) {
					saveToken(code);
					
					return "dashboard.html";
				}
		return "index.html";
	}
	
	@GetMapping(value = "/create")
	public void createFile(HttpServletResponse response) throws Exception {
		Credential cred = flow.loadCredential(USER_IDENTIFIER_KEY);
		
		Drive drive = new Drive.Builder(HTTP_TRANSPORT,JSON_FACTORY,cred).setApplicationName("googledrivespringbootexample").build(); // your application name in google api
		File file = new File();
		file.setName("sample.jpg");
		FileContent content = new FileContent("image/jpeg", new java.io.File("D:\\file\\profile.jpg"));
		File uploadedFile = drive.files().create(file, content).setFields("id").execute();
		
		String fileRef = String.format("fileID: '%s'", uploadedFile.getId());
		response.getWriter().write(fileRef);
	}

	private void saveToken(String code) throws IOException {
		GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
		flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);
	}
}
