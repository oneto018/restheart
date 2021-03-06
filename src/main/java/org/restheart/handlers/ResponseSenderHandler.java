/*
 * RESTHeart - the Web API for MongoDB
 * Copyright (C) SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.handlers;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.restheart.hal.Representation;
import org.restheart.utils.JsonUtils;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class ResponseSenderHandler extends PipedHttpHandler {
    /**
     * @param next
     */
    public ResponseSenderHandler() {
        super(null);
    }
    
    /**
     * @param next
     */
    public ResponseSenderHandler(PipedHttpHandler next) {
        super(next);
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(
            HttpServerExchange exchange,
            RequestContext context)
            throws Exception {
        BsonValue responseContent = context.getResponseContent();

        if (context.getWarnings() != null
                && !context.getWarnings().isEmpty()) {
            if (responseContent == null) {
                responseContent = new BsonDocument();
            }

            BsonArray warnings = new BsonArray();

            // add warnings, in hal o json format
            if (responseContent.isDocument()) {
                if (context.getRepresentationFormat()
                        == RequestContext.REPRESENTATION_FORMAT.PJ
                        || context.getRepresentationFormat()
                        == RequestContext.REPRESENTATION_FORMAT.PLAIN_JSON) {
                    context.setResponseContentType(Representation.JSON_MEDIA_TYPE);

                    responseContent.asDocument().append("_warnings", warnings);
                    context.getWarnings().forEach(
                            w -> warnings.add(new BsonString(w)));

                } else {
                    context.setResponseContentType(Representation.HAL_JSON_MEDIA_TYPE);

                    BsonDocument _embedded;
                    if (responseContent.asDocument().get("_embedded") == null) {
                        _embedded = new BsonDocument();
                        responseContent.asDocument().append("_embedded", _embedded);
                    } else {
                        _embedded = responseContent
                                .asDocument()
                                .get("_embedded")
                                .asDocument();
                    }

                    _embedded
                            .append("rh:warnings", warnings);

                    context.getWarnings().forEach(
                            w -> warnings.add(getWarningDoc(w)));
                }
            }
        }

        if (context.getRepresentationFormat()
                == RequestContext.REPRESENTATION_FORMAT.HAL) {
            exchange.getResponseHeaders().put(
                    Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
        } else {
            exchange.getResponseHeaders().put(
                    Headers.CONTENT_TYPE, Representation.JSON_MEDIA_TYPE);
        }

        exchange.setStatusCode(context.getResponseStatusCode());

        if (responseContent != null) {
            exchange.getResponseSender().send(
                    JsonUtils.toJson(responseContent));
        }

        exchange.endExchange();

        next(exchange, context);
    }

    private BsonDocument getWarningDoc(String warning) {
        Representation nrep = new Representation("#warnings");
        nrep.addProperty("message", new BsonString(warning));
        return nrep.asBsonDocument();
    }
}
