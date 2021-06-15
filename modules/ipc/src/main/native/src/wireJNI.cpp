/*
 * Copyright 2019-2021 tsurugi project.
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

#include <jni.h>
#include "com_nautilus_technologies_tsubakuro_impl_low_sql_SessionWireImpl.h"
#include "com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl.h"
#include "udf_wires.h"

using namespace tsubakuro::common::wire;

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_SessionWireImpl
 * Method:    openNative
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_SessionWireImpl_openNative
(JNIEnv *env, jclass, jstring name)
{
    session_wire_container* container;
    const char* name_ = env->GetStringUTFChars(name, NULL);
    if (name_ == NULL) return 0;
    jsize len_ = env->GetStringUTFLength(name);

    try {
        container = new session_wire_container(std::string_view(name_, len_));
    } catch (std::runtime_error &e) {
        env->ReleaseStringUTFChars(name, name_);
        jclass classj = env->FindClass("Ljava/io/IOException;");
        if (classj == nullptr) { std::abort(); }
        env->ThrowNew(classj, e.what());
        env->DeleteLocalRef(classj);
        return 0;
    }
    env->ReleaseStringUTFChars(name, name_);
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(container));
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_SessionWireImpl
 * Method:    sendNative
 * Signature: (JLjava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_SessionWireImpl_sendNative
(JNIEnv *env, jclass, jlong handle, jobject buf)
{
    session_wire_container* container = reinterpret_cast<session_wire_container*>(static_cast<std::uintptr_t>(handle));

    session_wire_container::response *r =
        container->write(static_cast<signed char*>(env->GetDirectBufferAddress(buf)),
                         env->GetDirectBufferCapacity(buf));
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(r));
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_SessionWireImpl
 * Method:    receiveNative
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_SessionWireImpl_receiveNative
(JNIEnv *env, jclass, jlong handle)
{
    session_wire_container::response *r = reinterpret_cast<session_wire_container::response*>(static_cast<std::uintptr_t>(handle));

    signed char* msg = r->read();

    if (msg == NULL) { return NULL; }
    return env->NewDirectByteBuffer(msg, r->get_length());
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_SessionWireImpl
 * Method:    closeNative
 * Signature: (J)Z
 */
JNIEXPORT void JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_SessionWireImpl_closeNative
(JNIEnv *, jclass, jlong handle)
{
    session_wire_container* container = reinterpret_cast<session_wire_container*>(static_cast<std::uintptr_t>(handle));

    if (container != nullptr) {
        delete container;
    }
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl
 * Method:    createNative
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_ResultSetWireImpl_createNative
(JNIEnv *env, jclass, jlong handle, jstring name)
{
    session_wire_container* c;

    try {
        c = reinterpret_cast<session_wire_container*>(static_cast<std::uintptr_t>(handle));
    } catch (std::runtime_error &e) {
        jclass classj = env->FindClass("Ljava/io/IOException;");
        if (classj == nullptr) { std::abort(); }
        env->ThrowNew(classj, e.what());
        env->DeleteLocalRef(classj);
    }

    const char* name_ = env->GetStringUTFChars(name, NULL);
    if (name_ == NULL) return 0;
    jsize len_ = env->GetStringUTFLength(name);

    session_wire_container::resultset_wire_container* container;
    try {
        container = c->create_resultset_wire(std::string_view(name_, len_));
    } catch (std::runtime_error &e) {
        jclass classj = env->FindClass("Ljava/io/IOException;");
        if (classj == nullptr) { std::abort(); }
        env->ThrowNew(classj, e.what());
        env->DeleteLocalRef(classj);
    }
    env->ReleaseStringUTFChars(name, name_);
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(container));
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl
 * Method:    receiveSchemaMetaDataNative
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_ResultSetWireImpl_receiveSchemaMetaDataNative
(JNIEnv *env, jclass, jlong handle)
{
    session_wire_container::resultset_wire_container* container = reinterpret_cast<session_wire_container::resultset_wire_container*>(static_cast<std::uintptr_t>(handle));

    auto b = container->recv_meta();
    return env->NewDirectByteBuffer(b.first, b.second);
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl
 * Method:    msgNative
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_ResultSetWireImpl_getChunkNative
(JNIEnv *env, jclass, jlong handle)
{
    session_wire_container::resultset_wire_container* container = reinterpret_cast<session_wire_container::resultset_wire_container*>(static_cast<std::uintptr_t>(handle));

    auto buf = container->get_chunk();
    if(buf.second > 0) {
        return env->NewDirectByteBuffer(buf.first, buf.second);
    }
    return nullptr;
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl
 * Method:    disposeUsedDataNative
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_ResultSetWireImpl_disposeUsedDataNative
(JNIEnv *, jclass, jlong handle, jlong length)
{
    session_wire_container::resultset_wire_container* container = reinterpret_cast<session_wire_container::resultset_wire_container*>(static_cast<std::uintptr_t>(handle));

    container->dispose(static_cast<std::size_t>(length));
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl
 * Method:    isEndOfRecordNative
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_ResultSetWireImpl_isEndOfRecordNative
(JNIEnv *, jclass, jlong handle)
{
    session_wire_container::resultset_wire_container* container = reinterpret_cast<session_wire_container::resultset_wire_container*>(static_cast<std::uintptr_t>(handle));

    return static_cast<jboolean>(container->is_eor());
}

/*
 * Class:     com_nautilus_technologies_tsubakuro_impl_low_sql_ResultSetWireImpl
 * Method:    closeNative
 * Signature: (J)Z
 */
JNIEXPORT void JNICALL Java_com_nautilus_1technologies_tsubakuro_impl_low_sql_ResultSetWireImpl_closeNative
(JNIEnv *, jclass, jlong handle)
{
    session_wire_container::resultset_wire_container* container = reinterpret_cast<session_wire_container::resultset_wire_container*>(static_cast<std::uintptr_t>(handle));

    if (container != nullptr) {
        session_wire_container* envelope = container->get_envelope();
        envelope->dispose_resultset_wire(container);
    }
}
