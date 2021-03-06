package us.forcecraft;

import static argo.jdom.JsonNodeFactories.array;
import static argo.jdom.JsonNodeFactories.field;
import static argo.jdom.JsonNodeFactories.object;
import static argo.jdom.JsonNodeFactories.string;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import argo.format.PrettyJsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;

/*
 * Methods for interacting with the Force.com REST API
 */

public class ForceRestClient {
	static JdomParser parser = new JdomParser();
	static PrettyJsonFormatter formatter = new PrettyJsonFormatter();
	JsonNode oauth;

    void login(String loginHost, String username, String password) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
			System.out.println("Logging in to "+loginHost+" as "+username);
			
            HttpPost httpPost = new HttpPost("https://"+loginHost+"/services/oauth2/token");

            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("grant_type", "password"));
            nvps.add(new BasicNameValuePair("username", username));
            nvps.add(new BasicNameValuePair("password", password));
            // TODO - get from config
            nvps.add(new BasicNameValuePair("client_id", "3MVG9Km_cBLhsuPzTtcGHsZpj9JylyezngYKNi.dNkSQmA0fAdwMD9OzkQEPFDJv1UgVF5tcERKtuiP5Yiin3"));
            nvps.add(new BasicNameValuePair("client_secret", "6135262856068035680"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                }

            };

            System.out.println("executing POST " + httpPost.getURI());
            
            String responseBody = httpclient.execute(httpPost, responseHandler);
            System.out.println("----------------------------------------");
            oauth = parser.parse(responseBody);
            Map<JsonStringNode,JsonNode> fieldMap = oauth.getFields();
            for (Map.Entry<JsonStringNode,JsonNode> entry : fieldMap.entrySet())
            {
                System.out.println(entry.getKey().getText() + ": " + entry.getValue().getText());
            }
            System.out.println("----------------------------------------");

        } finally {
            httpclient.close();
        }
    }
    
	JsonNode getAccounts() throws Exception {
		JsonRootNode root = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
        	String query = "SELECT Name, Id, IsDeleted,"+
    				"(SELECT Id, Name, Amount, StageName, IsClosed FROM Account.Opportunities ORDER BY CreatedDate), "+
        			"(SELECT Id, Name FROM Account.Contacts ORDER BY CreatedDate) "+
    				"FROM Account "+
        			"ORDER BY CreatedDate";
            HttpGet httpget = new HttpGet(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/query?q="+URLEncoder.encode(query, "UTF-8"));
            
            httpget.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    if (status >= 200 && status < 300) {
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                    	System.err.println("HTTP error "+status+"\n"+(entity != null ? EntityUtils.toString(entity) : ""));
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing GET " + httpget.getURI());

            String responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("----------------------------------------");
            root = parser.parse(responseBody);
            System.out.println(formatter.format(root));
            System.out.println("----------------------------------------");
        	
        } finally {
            httpclient.close();
        }
        
        return root;
	}
	
	List<JsonNode> getStages() throws Exception {
		JsonRootNode root = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/sobjects/Opportunity/describe");
            
            httpget.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing GET " + httpget.getURI());

            String responseBody = httpclient.execute(httpget, responseHandler);
            root = parser.parse(responseBody);
            
            for (JsonNode field : root.getNode("fields").getElements()) {
            	if (field.getStringValue("name").equals("StageName")) {
            		JsonNode picklistValues = field.getNode("picklistValues");
                    System.out.println("----------------------------------------");
                    System.out.println(formatter.format(picklistValues.getRootNode()));
                    System.out.println("----------------------------------------");
            		return picklistValues.getElements();
            	}
            }
        	
        } finally {
            httpclient.close();
        }
        
        return null;
	}
	
	void getId() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(oauth.getStringValue("id"));
            
            httpget.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing GET " + httpget.getURI());

            String responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("----------------------------------------");
            JsonRootNode root = parser.parse(responseBody);
            Map<JsonStringNode,JsonNode> fieldMap = root.getFields();
            for (Map.Entry<JsonStringNode,JsonNode> entry : fieldMap.entrySet())
            {
                //System.out.println(entry.getKey().getText() + ": " + entry.getValue().getText());
            }
            System.out.println("----------------------------------------");
            String displayName = root.getStringValue("display_name"); 
    		//player.addChatMessage("Hello " + displayName);

        } finally {
            httpclient.close();
        }		
	}
	
	public void setOpportunityStage(String id, String stage) {
		JsonRootNode root = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
        	HttpPost httppost = new HttpPost(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/sobjects/Opportunity/"+id+"?_HttpMethod=PATCH");
            
            httppost.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            httppost.setEntity(new StringEntity(formatter.format(object(field("StageName", string(stage)))),
                        ContentType.create("application/json", Consts.UTF_8)));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        return response.getStatusLine().toString();
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing POST " + httppost.getURI());

            String responseBody = httpclient.execute(httppost, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
            System.out.println("----------------------------------------");
		} catch (Exception e) {
			e.printStackTrace();
        } finally {
        	try {
        		httpclient.close();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
	}

	public boolean streamingTopicExists(String topicName) throws Exception {
		JsonRootNode root = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
			System.out.println("Checking for PushTopic "+topicName);
			
        	String query = "SELECT Id FROM PushTopic WHERE Name = '"+topicName+"'";
            HttpGet httpget = new HttpGet(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/query?q="+URLEncoder.encode(query, "UTF-8"));
            
            httpget.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing GET " + httpget.getURI());

            String responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("----------------------------------------");
            root = parser.parse(responseBody);
            System.out.println(formatter.format(root));
            System.out.println("----------------------------------------");
        	
        } finally {
            httpclient.close();
        }
        
        return (root != null && root.getNode("records").getElements().size() > 0);	
	}

	public void createStreamingTopic(String topicName, String topicQuery) throws Exception {
		JsonRootNode root = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
			System.out.println("Creating PushTopic "+topicName);
			
        	HttpPost httppost = new HttpPost(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/sobjects/PushTopic");
            
            httppost.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            JsonRootNode json = object(
                field("Name", string(topicName)),
                field("Query", string(topicQuery)),
                field("ApiVersion", string(StreamingClient.API_VERSION))
            );
            httppost.setEntity(new StringEntity(formatter.format(json),
                    ContentType.create("application/json", Consts.UTF_8)));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        return response.getStatusLine().toString();
                    } else {
                    	HttpEntity entity = response.getEntity();
                    	System.err.println("HTTP error "+status+"\n"+(entity != null ? EntityUtils.toString(entity) : ""));
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing POST " + httppost.getURI());

            String responseBody = httpclient.execute(httppost, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
            System.out.println("----------------------------------------");
		} catch (Exception e) {
			e.printStackTrace();
        } finally {
        	try {
        		httpclient.close();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
	}

	String getFeed(String recordId) throws Exception {
		String responseBody = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/chatter/feeds/record/"+recordId+"/feed-items");
            
            httpget.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing GET " + httpget.getURI());

            responseBody = httpclient.execute(httpget, responseHandler);
        } finally {
            httpclient.close();
        }
        
        return responseBody;
	}

	public void postToChatter(String recordId, String post) {
		JsonRootNode root = null;
		// Do an async request - we don't really care about the response, at least for now
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
			System.out.println("Creating Chatter post");
			
        	HttpPost httppost = new HttpPost(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/chatter/feeds/record/"+recordId+"/feed-items");
            
            httppost.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            JsonRootNode json = object(
                field("body", object(
                	field("messageSegments", array(
                		object(
                			field("type", string("Text")),
                            field("text", string(post))
                        )
                	))
                ))
            );
            httppost.setEntity(new StringEntity(formatter.format(json),
                    ContentType.create("application/json", Consts.UTF_8)));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        return response.getStatusLine().toString();
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing POST " + httppost.getURI());

            String responseBody = httpclient.execute(httppost, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
            System.out.println("----------------------------------------");
		} catch (Exception e) {
			e.printStackTrace();
        } finally {
        	try {
        		httpclient.close();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
	}
	
	public void delete(String objectType, String recordId) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpDelete httpdelete = new HttpDelete(oauth.getStringValue("instance_url")+
            		"/services/data/v29.0/sobjects/"+objectType+"/"+recordId);
            
            httpdelete.addHeader("Authorization", "Bearer "+oauth.getStringValue("access_token"));
            

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        return response.getStatusLine().toString();
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            System.out.println("executing DELETE " + httpdelete.getURI());

            String responseBody = httpclient.execute(httpdelete, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
            System.out.println("----------------------------------------");
        } finally {
            httpclient.close();
        }
	}
}
