package io.vertx.starter.rest;

import com.cyngn.vertx.async.ResultContext;
import com.cyngn.vertx.web.HttpHelper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.lang.Boolean;
import java.lang.String;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GENERATED CODE DO NOT MODIFY - last updated: 2017-02-20T06:11:58.831Z
 * generated by exovert - https://github.com/cyngn/exovert
 *
 * Central place to put shared functions for REST call processing.
 */
public class RestUtil {
  private static final Logger logger = LoggerFactory.getLogger(RestUtil.class);

  /**
   * Does the query string of this request contain the full primary key?
   */
  public static Boolean isValid(final HttpServerRequest request, final String[] primaryKey) {
    String error = null;
    for(int i = 0; i < primaryKey.length; i++) {
      String key = primaryKey[i];
      String value = request.getParam(key);
      if(StringUtils.isEmpty(value)) {
        error = "You must supply parameter: " + key;
        HttpHelper.processErrorResponse(error, request.response(), HttpResponseStatus.BAD_REQUEST.code());
        break;
      }
    }
    return StringUtils.isEmpty(error);
  }

  /**
   * Handles processing a get all result
   */
  public static <T> void processGetAllResult(final RoutingContext context, final ResultContext<List<T>> result) {
    if(result.succeeded) {
      if(result.value != null) {
        HttpHelper.processResponse(result.value, context.response());
      } else {
        HttpHelper.processResponse(context.response(), HttpResponseStatus.NOT_FOUND.code());
      }
    } else if(result.error != null) {
      String error = "Could not GET with query: " + context.request().uri() + " error: " + result.error.getMessage();
      logger.error("get_all - {}",  error);
      HttpHelper.processErrorResponse(error, context.response(), HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
    } else {
      String error = "Could not GET with query: " + context.request().uri() + " error: " + result.error.getMessage();
      logger.error("get_all - {}", error);
      HttpHelper.processErrorResponse(error, context.response(), HttpResponseStatus.BAD_REQUEST.code());
    }
  }
}