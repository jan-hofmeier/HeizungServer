package de.recondita.heizung.server.GoogleSheet;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

public class SheetRoomConfig {

    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    
	public SheetRoomConfig() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		final GoogleCredentials credential =
		        ServiceAccountCredentials.fromStream(new FileInputStream("config/google-credentials.json"))
		        .createScoped(SheetsScopes.SPREADSHEETS);
		
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,  new HttpCredentialsAdapter(credential))
                .setApplicationName(APPLICATION_NAME)
                .build();
        
        
        ValueRange response = service.spreadsheets().values()
                .get("1HyVkpQfcoL511u5ev4pG0ycxLUA4YPzN0-9sRO-aYRM", "Räume")
                .execute();
        
        List<List<Object>> values = response.getValues();
        
        System.out.println(values);

	}

}
