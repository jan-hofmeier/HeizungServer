package de.recondita.heizung.server.GoogleSheet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class SheetRoomSettings {

    private static final String APPLICATION_NAME = "Heizung";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private String sheetId;
    private Sheets service;
    
	public SheetRoomSettings(String sheetId) throws FileNotFoundException, IOException, GeneralSecurityException {
		this.sheetId = sheetId;
		
		final GoogleCredentials credential =
		        ServiceAccountCredentials.fromStream(new FileInputStream("config/google-credentials.json"))
		        .createScoped(SheetsScopes.SPREADSHEETS);
		
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,  new HttpCredentialsAdapter(credential))
                .setApplicationName(APPLICATION_NAME)
                .build();
	}

	public List<List<Object>> getConfig() throws IOException{
        ValueRange response = service.spreadsheets().values()
                .get(sheetId, "Räume")
                .execute();
        
        return response.getValues();
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException {

        SheetRoomSettings rooms = new SheetRoomSettings("1HyVkpQfcoL511u5ev4pG0ycxLUA4YPzN0-9sRO-aYRM");    
        
        System.out.println(rooms.getConfig());
        
        Thread.sleep(600000);
        
        System.out.println(rooms.getConfig());
        
	}

}
