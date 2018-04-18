package com.r3.corda.finance.cash.issuer.client.services

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AbstractUser
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

class MySimpleAuthProvider : AuthProvider {
    override fun authenticate(authInfo: JsonObject, callback: Handler<AsyncResult<User>>) {
        try {
            val username = authInfo.getString("username") ?: throw RuntimeException("no username found")
            callback.handle(Future.succeededFuture(MySimpleUser(username)))
        } catch (err: Throwable) {
            callback.handle(Future.failedFuture(err))
        }
    }
}

class MySimpleUser(private val userName: String) : AbstractUser() {
    override fun doIsPermitted(permission: String, callback: Handler<AsyncResult<Boolean>>) {
        callback.handle(Future.succeededFuture(true))
    }

    override fun principal(): JsonObject {
        val result = JsonObject()
        result.put("username", userName)
        return result
    }

    override fun setAuthProvider(provider: AuthProvider) {}
}
