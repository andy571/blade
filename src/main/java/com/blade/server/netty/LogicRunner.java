/**
 * Copyright (c) 2018, biezhi 王爵 nice (biezhi.me@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blade.server.netty;

import com.blade.kit.BladeCache;
import com.blade.mvc.WebContext;
import com.blade.mvc.http.Request;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static com.blade.kit.BladeKit.log200;
import static com.blade.kit.BladeKit.log200AndCost;
import static com.blade.mvc.Const.REQUEST_COST_TIME;

/**
 * Http Logic Runner
 *
 * @author biezhi
 * @date 2018/10/15
 */
@Slf4j
public class LogicRunner {

    private CompletableFuture<Void> future;
    private WebContext              webContext;
    private Instant                 started;
    private RouteMethodHandler      routeHandler;
    private boolean                 isFinished;

    public LogicRunner(RouteMethodHandler routeHandler, WebContext webContext) {
        this.routeHandler = routeHandler;
        this.webContext = webContext;
        if (!HttpServerHandler.PERFORMANCE && HttpServerHandler.ALLOW_COST) {
            this.started = Instant.now();
        }
    }

    /**
     * Routing logic execution
     */
    public LogicRunner handle() {
        WebContext.set(webContext);
        Request request = webContext.getRequest();
        String  uri     = request.uri();
        String  method  = request.method();
        try {
            routeHandler.handle(webContext);

            if (HttpServerHandler.PERFORMANCE) {
                return this;
            }

            if (HttpServerHandler.ALLOW_COST) {
                long cost = log200AndCost(log, this.started, BladeCache.getPaddingMethod(method), uri);
                request.attribute(REQUEST_COST_TIME, cost);
            } else {
                log200(log, BladeCache.getPaddingMethod(method), uri);
            }
        } catch (Exception e) {
            routeHandler.exceptionCaught(uri, method, e);
        }
        return this;
    }

    public void finishWrite() {
        WebContext.set(webContext);
        routeHandler.finishWrite(webContext);
        WebContext.remove();
        isFinished = true;
        if (null != future) {
            future.complete(null);
        }
    }

    public void setFuture(CompletableFuture<Void> future) {
        this.future = future;
        if (isFinished) {
            future.complete(null);
        }
    }

}
