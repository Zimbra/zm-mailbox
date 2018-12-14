/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.redis;

import java.util.List;

import org.redisson.api.RFuture;
import org.redisson.api.RScript;
import org.redisson.client.codec.Codec;

public class RedissonRetryScript extends RedissonRetryDecorator<RScript> implements RScript {

    public RedissonRetryScript(RedissonInitializer<RScript> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }

    @Override
    public RFuture<Void> scriptFlushAsync() {
        return runCommand(() -> redissonObject.scriptFlushAsync());
    }

    @Override
    public <R> RFuture<R> evalShaAsync(Mode mode, String shaDigest,
ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalShaAsync(mode, shaDigest, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalShaAsync(Mode mode, Codec codec,
            String shaDigest, ReturnType returnType, List<Object> keys,
            Object... values) {
        return runCommand(() -> redissonObject.evalShaAsync(mode, codec, shaDigest, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalShaAsync(String key, Mode mode, String shaDigest,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalShaAsync(key, mode, shaDigest, returnType, keys ,values));
    }

    @Override
    public <R> RFuture<R> evalShaAsync(String key, Mode mode, Codec codec,
            String shaDigest, ReturnType returnType, List<Object> keys,
            Object... values) {
        return runCommand(() -> redissonObject.evalShaAsync(key, mode, codec, shaDigest, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalShaAsync(Mode mode, String shaDigest, ReturnType returnType) {
        return runCommand(() -> redissonObject.evalShaAsync(mode, shaDigest, returnType));
    }

    @Override
    public <R> RFuture<R> evalShaAsync(Mode mode, Codec codec,
            String shaDigest, ReturnType returnType) {
        return runCommand(() -> redissonObject.evalShaAsync(mode, codec, shaDigest, returnType));
    }

    @Override
    public <R> RFuture<R> evalAsync(Mode mode, String luaScript,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalAsync(mode, luaScript, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalAsync(Mode mode, Codec codec, String luaScript,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalAsync(mode, codec, luaScript, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalAsync(String key, Mode mode, Codec codec,
            String luaScript, ReturnType returnType, List<Object> keys,
            Object... values) {
        return runCommand(() -> redissonObject.evalAsync(key, mode, codec, luaScript, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalAsync(String key, Mode mode, String luaScript,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalAsync(key, mode, luaScript, returnType, keys, values));
    }

    @Override
    public <R> RFuture<R> evalAsync(Mode mode, String luaScript, ReturnType returnType) {
        return runCommand(() -> redissonObject.evalAsync(mode, luaScript, returnType));
    }

    @Override
    public <R> RFuture<R> evalAsync(Mode mode, Codec codec, String luaScript, ReturnType returnType) {
        return runCommand(() -> redissonObject.evalAsync(mode, codec, luaScript, returnType));
    }

    @Override
    public RFuture<String> scriptLoadAsync(String luaScript) {
        return runCommand(() -> redissonObject.scriptLoadAsync(luaScript));
    }

    @Override
    public RFuture<String> scriptLoadAsync(String key, String luaScript) {
        return runCommand(() -> redissonObject.scriptLoadAsync(key, luaScript));
    }

    @Override
    public RFuture<List<Boolean>> scriptExistsAsync(String... shaDigests) {
        return runCommand(() -> redissonObject.scriptExistsAsync(shaDigests));
    }

    @Override
    public RFuture<List<Boolean>> scriptExistsAsync(String key, String... shaDigests) {
        return runCommand(() -> redissonObject.scriptExistsAsync(key, shaDigests));
    }

    @Override
    public RFuture<Void> scriptKillAsync() {
        return runCommand(() -> redissonObject.scriptKillAsync());
    }

    @Override
    public <R> R evalSha(Mode mode, String shaDigest, ReturnType returnType,
            List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalSha(mode, shaDigest, returnType, keys, values));
    }

    @Override
    public <R> R evalSha(String key, Mode mode, String shaDigest,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalSha(key, mode, shaDigest, returnType, keys, values));
    }

    @Override
    public <R> R evalSha(Mode mode, Codec codec, String shaDigest,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.evalSha(mode, codec, shaDigest, returnType, keys, values));
    }

    @Override
    public <R> R evalSha(Mode mode, String shaDigest, ReturnType returnType) {
        return runCommand(() -> redissonObject.evalSha(mode, shaDigest, returnType));
    }

    @Override
    public <R> R evalSha(Mode mode, Codec codec, String shaDigest, ReturnType returnType) {
        return runCommand(() -> redissonObject.evalSha(mode, codec, shaDigest, returnType));
    }

    @Override
    public <R> R eval(String key, Mode mode, String luaScript,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.eval(key, mode, luaScript, returnType, keys, values));
    }

    @Override
    public <R> R eval(Mode mode, String luaScript, ReturnType returnType,
            List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.eval(mode, luaScript, returnType, keys, values));
    }

    @Override
    public <R> R eval(Mode mode, Codec codec, String luaScript,
            ReturnType returnType, List<Object> keys, Object... values) {
        return runCommand(() -> redissonObject.eval(mode, codec, luaScript, returnType, keys, values));
    }

    @Override
    public <R> R eval(Mode mode, String luaScript, ReturnType returnType) {
        return runCommand(() -> redissonObject.eval(mode, luaScript, returnType));
    }

    @Override
    public <R> R eval(Mode mode, Codec codec, String luaScript, ReturnType returnType) {
        return runCommand(() -> redissonObject.eval(mode, codec, luaScript, returnType));
    }

    @Override
    public String scriptLoad(String luaScript) {
        return runCommand(() -> redissonObject.scriptLoad(luaScript));
    }

    @Override
    public List<Boolean> scriptExists(String... shaDigests) {
        return runCommand(() -> redissonObject.scriptExists(shaDigests));
    }

    @Override
    public void scriptKill() {
        runCommand(() -> { redissonObject.scriptKill(); return null; });

    }

    @Override
    public void scriptFlush() {
        runCommand(() -> { redissonObject.scriptFlush(); return null; });
    }

}
