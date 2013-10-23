/**
 * 
 */
package com.app2square.core.api.parse;

import java.util.Map;

import playn.core.Json.Writer;
import playn.core.Net;
import playn.core.Net.Builder;
import playn.core.Net.Response;
import playn.core.PlayN;

import com.app2square.core.api.parse.ParseFactory;
import com.app2square.core.api.parse.json.Json;
import com.app2square.core.api.parse.json.JsonParseException;
import com.app2square.core.api.parse.net.HttpConnectionFactory;
import com.app2square.core.api.parse.net.UrlEncoder;

/**
 * @author cs
 * 
 */
public class PlaynParseFactory extends ParseFactory {

    public PlaynParseFactory(final String applicationId, final String restApiKey) {
        super(PlaynParseFactory.connectionFactory(), PlaynParseFactory.json(), PlaynParseFactory.encoder(),
                applicationId, restApiKey);
    }

    private static HttpConnectionFactory connectionFactory() {
        return new HttpConnectionFactory() {

            public void put(final String url, final String payload, final Map<String, String> headers,
                    final Callback callback) {
                throw new UnsupportedOperationException("Currently not supported");
            }

            public void post(final String url, final String payload, final Map<String, String> headers,
                    final Callback callback) {

                final Builder builder = PlayN.net().req(url).setPayload(payload);
                for (final Map.Entry<String, String> header : headers.entrySet()) {
                    builder.addHeader(header.getKey(), header.getValue());
                }
                builder.execute(new playn.core.util.Callback<Net.Response>() {

                    public void onSuccess(final Response result) {
                        callback.onSucceed(result.responseCode(), result.payloadString());
                    }

                    public void onFailure(final Throwable cause) {
                        callback.onExceptionThrown(cause);
                    }
                });
            }

            public void get(final String url, final Map<String, String> headers, final Callback callback) {
                final Builder builder = PlayN.net().req(url);
                for (final Map.Entry<String, String> header : headers.entrySet()) {
                    builder.addHeader(header.getKey(), header.getValue());
                }
                builder.execute(new playn.core.util.Callback<Net.Response>() {

                    public void onSuccess(final Response result) {
                        callback.onSucceed(result.responseCode(), result.payloadString());
                    }

                    public void onFailure(final Throwable cause) {
                        callback.onExceptionThrown(cause);
                    }
                });
            }

            public void delete(final String url, final Map<String, String> headers, final Callback callback) {
                throw new UnsupportedOperationException("Currently not supported");
            }
        };
    }

    private static Json json() {

        return new Json() {

            public String toJsonString(final Json.Object object) {
                Writer writer = PlayN.json().newWriter();

                for (final Map.Entry<String, Json.Element> element : object.entrySet()) {
                    if (element.getValue() instanceof Json.Array) {
                        writer = writer.array(element.getKey(), Converter.from((Json.Array) element.getValue()));
                    } else if (element.getValue() instanceof Json.Object) {
                        writer = writer.object(element.getKey(), Converter.from((Json.Object) element.getValue()));
                    } else {
                        writer = writer.value(element.getKey(), Converter.fromElement(element.getValue()));
                    }

                }

                return writer.write();
            }

            public Element parse(final String string) throws JsonParseException {

                try {
                    return Converter.toObject(PlayN.json().parse(string));
                } catch (final playn.core.json.JsonParserException e) {

                    try {
                        return Converter.toArray(PlayN.json().parseArray(string));

                    } catch (final playn.core.json.JsonParserException e1) {
                        throw new JsonParseException("Unable to parse String: " + string, e);
                    }
                }
            }

        };
    }

    private static final class Converter {

        private static playn.core.Json.Array from(final Json.Array array) {
            final playn.core.Json.Array playnArray = PlayN.json().createArray();
            for (final Json.Element element : array) {
                playnArray.add(Converter.fromElement(element));
            }
            return playnArray;
        }

        private static playn.core.Json.Object from(final Json.Object object) {
            final playn.core.Json.Object playnObject = PlayN.json().createObject();
            for (final Map.Entry<String, Json.Element> entry : object.entrySet()) {
                playnObject.put(entry.getKey(), Converter.fromElement(entry.getValue()));
            }
            return playnObject;
        }

        private static java.lang.Object fromElement(final Json.Element element) {

            if (element instanceof Json.Primitive) {
                return ((Json.Primitive) element).asObject();
            } else if (element instanceof Json.Object) {
                return Converter.from((Json.Object) element);
            } else if (element instanceof Json.Array) {
                return Converter.from((Json.Array) element);
            } else {
                throw new IllegalArgumentException();
            }
        }

        private static Json.Object toObject(final playn.core.Json.Object jsonObject) {

            final Json.Object object = new Json.Object();
            for (final String key : jsonObject.keys()) {

                if (jsonObject.isArray(key)) {
                    object.put(key, Converter.toArray(jsonObject.getArray(key)));
                } else if (jsonObject.isBoolean(key)) {
                    object.put(key, Converter.toPrimitive(jsonObject.getBoolean(key)));
                } else if (jsonObject.isNull(key)) {
                    throw new IllegalArgumentException("Unknown type of external json object: " + key + "; value: "
                            + jsonObject);
                } else if (jsonObject.isNumber(key)) {
                    object.put(key, Converter.toPrimitive(jsonObject.getNumber(key)));
                } else if (jsonObject.isObject(key)) {
                    object.put(key, Converter.toObject(jsonObject.getObject(key)));
                } else if (jsonObject.isString(key)) {
                    object.put(key, Converter.toPrimitive(jsonObject.getString(key)));
                } else {
                    throw new IllegalArgumentException("Unknown type of external json object: " + key + "; value: "
                            + jsonObject);
                }
            }
            return object;
        }

        private static Json.Primitive toPrimitive(final java.lang.Object object) {
            return new Json.Primitive(object);
        }

        private static Json.Array toArray(final playn.core.Json.Array jsonArray) {
            final Json.Array array = new Json.Array();

            for (int i = 0; i < jsonArray.length(); i++) {

                if (jsonArray.isArray(i)) {
                    array.add(Converter.toArray(jsonArray.getArray(i)));
                } else if (jsonArray.isBoolean(i)) {
                    array.add(Converter.toPrimitive(jsonArray.getBoolean(i)));
                } else if (jsonArray.isNull(i)) {
                    // TODO implement null value. 
                    throw new IllegalArgumentException("TODO ");
                } else if (jsonArray.isNumber(i)) {
                    array.add(Converter.toPrimitive(jsonArray.getNumber(i)));
                } else if (jsonArray.isObject(i)) {
                    array.add(Converter.toObject(jsonArray.getObject(i)));
                } else if (jsonArray.isString(i)) {
                    array.add(Converter.toPrimitive(jsonArray.getString(i)));
                } else {
                    throw new IllegalArgumentException("Unknown type of external json object: " + i + "; value: "
                            + jsonArray);
                }
            }

            return array;
        }
    }

    private static UrlEncoder encoder() {
        return new UrlEncoder() {

            public String encode(final String s) {
                // TODO does playn the URL encoding in the net() backend? 
                return s;
            }
        };
    }
}
