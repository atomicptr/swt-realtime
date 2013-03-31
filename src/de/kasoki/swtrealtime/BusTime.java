package de.kasoki.swtrealtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class BusTime {
	
	public static BusTime fromStopCode(String stopCode) {
		String url = "http://212.18.193.124/onlineinfo/onlineinfo/stopData";
		String charset = "UTF-8";
		
		String response = "";
		
		try {
			// setup connection
			URLConnection connection = new URL(url).openConnection();
			connection.setDoOutput(true); // triggers POST
			
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("X-GWT-Module-Base", "http://212.18.193.124/onlineinfo/onlineinfo/");
			connection.setRequestProperty("X-GWT-Permutation", "D8AB656D349DD625FC1E4BA18B0A253C");
			connection.setRequestProperty("Content-Type", "text/x-gwt-rpc; charset=" + charset);
			
			String body = "5|0|6|http://212.18.193.124/onlineinfo/onlineinfo/|7E201FB9D23B0EA0BDBDC82C554E92FE|com.initka.onlineinfo.client.services.StopDataService|getDepartureInformationForStop|java.lang.String/2004016611|%s|1|2|3|4|1|5|6|";
			body = String.format(body, stopCode);
			
			OutputStream output = connection.getOutputStream();
			output.write(body.getBytes());
			output.close();
			
			connection.connect();
			
			// read connection
			InputStream input = connection.getInputStream();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
			
			for (String line; (line = reader.readLine()) != null;) {
	            response += line;
	        }
			
			reader.close();
			
			System.out.println(response);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new BusTime();
	}
	
	public static void main(String[] args) {
		BusTime time = BusTime.fromStopCode("HBF");
	}
}
