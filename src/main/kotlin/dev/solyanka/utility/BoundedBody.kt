/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.io.ByteArrayOutputStream
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.util.concurrent.*

/** Bounded subscriber: enforce the cap even when the server omits or lies about Content-Length. */
class BoundedBody(private val limit:Int):HttpResponse.BodySubscriber<ByteArray> {
    init { require(limit>0) }
    private val result=CompletableFuture<ByteArray>()
    private val output=ByteArrayOutputStream()
    private var subscription:Flow.Subscription?=null
    override fun getBody():CompletionStage<ByteArray> = result
    override fun onSubscribe(s:Flow.Subscription) {
        if(subscription!=null) { s.cancel();return }
        subscription=s;s.request(1)
    }
    override fun onNext(buffers:List<ByteBuffer>) {
        if(result.isDone) return
        try {
            buffers.forEach { buffer ->
                require(buffer.remaining()<=limit-output.size()) { "HTTP resource exceeds $limit bytes" }
                val bytes=ByteArray(buffer.remaining());buffer.get(bytes);output.write(bytes)
            }
            subscription?.request(1)
        } catch(e:Exception) { subscription?.cancel();result.completeExceptionally(e) }
    }
    override fun onError(t:Throwable) { result.completeExceptionally(t) }
    override fun onComplete() { result.complete(output.toByteArray()) }
    companion object {
        fun handler(limit:Int)=HttpResponse.BodyHandler<ByteArray> { BoundedBody(limit) }
    }
}
