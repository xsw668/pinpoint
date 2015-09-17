/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.plugin;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.instrument.ClassFileTransformer;

import org.junit.Test;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.group.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.interceptor.group.InterceptorGroup;
import com.navercorp.pinpoint.profiler.DefaultAgent;
import com.navercorp.pinpoint.profiler.instrument.JavassistClassPool;
import com.navercorp.pinpoint.profiler.plugin.xml.transformer.DefaultClassFileTransformerBuilder;
import com.navercorp.pinpoint.profiler.plugin.xml.transformer.MethodTransformerBuilder;
import com.navercorp.pinpoint.profiler.util.TypeUtils;
import com.navercorp.pinpoint.test.TestProfilerPluginClassLoader;

public class DefaultClassEditorBuilderTest {
    public static final String SCOPE_NAME = "test";

    @Test
    public void test() throws Exception {
        JavassistClassPool pool = mock(JavassistClassPool.class);
        TraceContext traceContext = mock(TraceContext.class);
        InstrumentClass aClass = mock(InstrumentClass.class);
        InstrumentMethod aMethod = mock(InstrumentMethod.class);
        MethodDescriptor aDescriptor = mock(MethodDescriptor.class);
        DefaultAgent agent = mock(DefaultAgent.class);
        DefaultProfilerPluginContext context = new DefaultProfilerPluginContext(agent, new TestProfilerPluginClassLoader());
        
        ClassLoader classLoader = getClass().getClassLoader();
        String className = "someClass";
        String methodName = "someMethod";
        byte[] classFileBuffer = new byte[0];
        Class<?>[] parameterTypes = new Class<?>[] { String.class };
        String[] parameterTypeNames = TypeUtils.toClassNames(parameterTypes);
        
        when(agent.getClassPool()).thenReturn(pool);
        when(agent.getTraceContext()).thenReturn(traceContext);
        when(pool.getClass(context, classLoader, className, classFileBuffer)).thenReturn(aClass);
        when(aClass.getDeclaredMethod(methodName, parameterTypeNames)).thenReturn(aMethod);
        when(aMethod.getName()).thenReturn(methodName);
        when(aMethod.getParameterTypes()).thenReturn(parameterTypeNames);
        when(aMethod.getDescriptor()).thenReturn(aDescriptor);
        when(aClass.addInterceptor(eq(methodName), eq(parameterTypeNames), isA(Interceptor.class))).thenReturn(0);
        
        
        DefaultClassFileTransformerBuilder builder = new DefaultClassFileTransformerBuilder(context, "TargetClass");
        builder.injectField("some.accessor.Type", "java.util.HashMap");
        builder.injectGetter("some.getter.Type", "someField");
        
        MethodTransformerBuilder ib = builder.editMethod(methodName, parameterTypeNames);
        ib.injectInterceptor("com.navercorp.pinpoint.profiler.plugin.TestInterceptor", "provided");
        
        ClassFileTransformer transformer = builder.build();
        
        transformer.transform(classLoader, className, null, null, classFileBuffer);
        
        verify(aMethod).addInterceptor(eq("com.navercorp.pinpoint.profiler.plugin.TestInterceptor"), (InterceptorGroup)isNull(), (ExecutionPolicy)isNull(), eq("provided"));
        verify(aClass).addField("some.accessor.Type", "new java.util.HashMap();");
        verify(aClass).addGetter("some.getter.Type", "someField");
    }
}
