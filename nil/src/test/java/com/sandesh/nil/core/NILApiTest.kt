package com.sandesh.nil.core

import org.junit.Assert.assertSame
import org.junit.Test

class NILApiTest {
    @Test
    fun interceptor_enumOverload_returnsSingletonInterceptor() {
        val defaultInterceptor = NIL.interceptor()
        val httpUrlInterceptor = NIL.interceptor(NIL.InterceptorType.HTTP_URL_CONNECTION)
        val okHttpInterceptor = NIL.interceptor(NIL.InterceptorType.OK_HTTP)

        assertSame(defaultInterceptor, httpUrlInterceptor)
        assertSame(defaultInterceptor, okHttpInterceptor)
    }

    @Suppress("DEPRECATION")
    @Test
    fun interceptor_stringOverload_preservesBackwardCompatibility() {
        val defaultInterceptor = NIL.interceptor()

        assertSame(defaultInterceptor, NIL.interceptor("okhttp"))
        assertSame(defaultInterceptor, NIL.interceptor("httpURL"))
        assertSame(defaultInterceptor, NIL.interceptor("httpurlconnection"))
    }
}
