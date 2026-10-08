package com.tacz.guns.security;

import com.google.gson.Gson;
import com.tacz.guns.api.modifier.ParameterizedCache;
import com.tacz.guns.resource.manager.AttachmentScriptManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import java.util.*;
import java.util.concurrent.*;

/** Executes only the official restricted attachment-script environment. */
public final class AttachmentScriptChecks {
    static int assertions;
    static void check(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        check(AttachmentScriptManager.compile(null)==null, "Null script");
        check(AttachmentScriptManager.compile("")==null, "Empty script");
        var script=AttachmentScriptManager.compile("Y=X*2+R");
        check(script!=null && script.eval(4,3)==11, "Uppercase-compatible arithmetic");
        check(script.eval(7,9)==23, "Fresh evaluation variables");
        var noOutput=AttachmentScriptManager.compile("local z=x+1");
        check(noOutput.eval(4,8)==4, "No stale y from prior script");
        var forbidden=AttachmentScriptManager.compile("y=(package==nil and io==nil and os==nil and luajava==nil and debug==nil and coroutine==nil and load==nil and loadfile==nil and dofile==nil and getmetatable==nil and print==nil and collectgarbage==nil) and 1 or 0");
        check(forbidden.eval(0,0)==1, "Unsafe modules and loaders absent");
        var libs=AttachmentScriptManager.compile("y=math.floor(x)+string.len('ab')+bit32.band(7,3)");
        check(libs.eval(2.5,0)==7, "Allowed math/string/bit libraries");
        Modifier modifier=new Gson().fromJson("{\"addend\":2,\"percent\":0.5,\"multiplier\":2,\"function\":\"y=x+r\"}",Modifier.class);
        check(modifier.getCompiledFunction()!=null, "JSON precompiles script");
        var cache=new ParameterizedCache<Double>(List.of(modifier),10.0);
        check(cache.eval(10)==46, "Compiled arithmetic modifier path");
        check(cache.eval(10,1,0.5,2)==114, "Extra modifiers compiled path");
        try(var executor=Executors.newFixedThreadPool(4)) {
            List<Future<Double>> results=new ArrayList<>();
            for(int i=0;i<100;i++) { final int n=i;results.add(executor.submit(()->script.eval(n,n+1))); }
            for(int i=0;i<results.size();i++)check(results.get(i).get()==i*3+1, "Concurrent isolated evaluation "+i);
        }
        System.out.println("PASS "+assertions+" restricted attachment-script assertions");
    }
}
