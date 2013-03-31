package de.kasoki.swtrealtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

public class BusTime {
	
	private int number;
	private String destination;
	private Date arrivalTime;
	private Date expectedArrivalTime;
	
	private SimpleDateFormat dateFormat;
	
	private BusTime(int number, String destination, Date arrivalTime, Date expectedArrivalTime) {
		this.dateFormat = new SimpleDateFormat("HH:MM");
		
		this.number = number;
		this.destination = destination;
		this.arrivalTime = arrivalTime;
		this.expectedArrivalTime = expectedArrivalTime;
	}
	
	@Override
	public String toString() {
		return number + ": " + destination + " [Arrival Time: " +
				dateFormat.format(arrivalTime) + " / Expected: " + dateFormat.format(expectedArrivalTime) + " ]";
	}
	
	public static List<BusTime> fromStopCode(String stopCode) {
		String url = "http://212.18.193.124/onlineinfo/onlineinfo/stopData";
		String charset = "UTF-8";
		
		String response = "";
		
		List<BusTime> busTimeList = null;
		
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
			
			// parse response
			busTimeList = BusTime.parseResponse(response);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return busTimeList;
	}
	
	private static List<BusTime> parseResponse(String response) throws Exception {
		List<BusTime> busTimeList = new ArrayList<BusTime>();
		
		if(response.startsWith("//OK")) {
			String jsonString = response.substring(4, response.length());
			
			try {
				JSONArray json = new JSONArray(jsonString);
				
				JSONArray innerInformations = new JSONArray(json.get(json.length() - 3).toString());
				
				for(int i = 0; i < Math.floor(json.length() / 11); i++) {
					int number = Integer.parseInt(getItemFromInnerInformationList(innerInformations, json.getInt(i * 11 + 5)));
					String destination = getItemFromInnerInformationList(innerInformations, json.getInt(i * 11 + 6));
					Date arrivalTime = new Date(json.getLong(i * 11 + 2) + json.getLong(i * 11 + 3));
					Date expectedArrivalTime = new Date(json.getLong(i * 11 + 7) + json.getLong(i * 11 + 8));
					
					busTimeList.add(new BusTime(number, destination, arrivalTime, expectedArrivalTime));
				}
			} catch(JSONException e) {
				e.printStackTrace();
			}
		} else {
			throw new Exception("Error: Invalid Response: " + response);
		}
		
		return busTimeList;
	}
	
	private static String getItemFromInnerInformationList(JSONArray innerInformations, int index) {
		return innerInformations.getString(index - 1);
	}
	
	public static void main(String[] args) {
		List<BusTime> list = BusTime.fromStopCode("aache");
		
		for(BusTime busTime : list) {
			System.out.println(busTime);
		}
	}
}
