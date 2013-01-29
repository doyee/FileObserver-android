/* //device/libs/android_runtime/android_util_FileObserver.cpp
 **
 ** Copyright 2006, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include "logger.h"

#define HAVE_INOTIFY
#ifdef HAVE_INOTIFY
#include <sys/inotify.h>
#endif

#define LOG_TAG "libfileobserver"

static jmethodID method_onEvent;

static jint Native_init(JNIEnv* env, jobject object)
{
#ifdef HAVE_INOTIFY

	return (jint)inotify_init();

#else // HAVE_INOTIFY
	return -1;

#endif // HAVE_INOTIFY
}

static void Native_observe(JNIEnv* env, jobject object, jint fd)
{
#ifdef HAVE_INOTIFY

	char event_buf[512];
	struct inotify_event* event;

	while (1)
	{
		int event_pos = 0;
		int num_bytes = read(fd, event_buf, sizeof(event_buf));

		if (num_bytes < (int)sizeof(*event))
		{
			if (errno == EINTR)
			continue;

			LOGE("***** ERROR! Native_observe() got a short event!");
			return;
		}

		while (num_bytes >= (int)sizeof(*event))
		{
			int event_size;
			event = (struct inotify_event *)(event_buf + event_pos);

			jstring path = NULL;

			if (event->len > 0)
			{
				path = env->NewStringUTF(event->name);
			}

			env->CallVoidMethod(object, method_onEvent, event->wd, event->mask,event->cookie,path);
			if (env->ExceptionCheck())
			{
				env->ExceptionDescribe();
				env->ExceptionClear();
			}
			if (path != NULL)
			{
				env->DeleteLocalRef(path);
			}

			event_size = sizeof(*event) + event->len;
			num_bytes -= event_size;
			event_pos += event_size;
		}
	}

#endif // HAVE_INOTIFY
}

static jint Native_startWatching(JNIEnv* env, jobject object, jint fd,
		jstring pathString, jint mask)
{
	int res = -1;

#ifdef HAVE_INOTIFY

	if (fd >= 0)
	{
		const char* path = env->GetStringUTFChars(pathString, NULL);

		res = inotify_add_watch(fd, path, mask);

		//LOGD("inotify_add_watch res %d,errno:%d",res,errno);
		env->ReleaseStringUTFChars(pathString, path);
	}

#endif // HAVE_INOTIFY
	return res;
}

static void Native_stopWatching(JNIEnv* env, jobject object, jint fd, jint wfd)
{
#ifdef HAVE_INOTIFY

	inotify_rm_watch((int)fd, (uint32_t)wfd);

#endif // HAVE_INOTIFY
}

static JNINativeMethod sMethods[] =
{
/* name, signature, funcPtr */
{ "init", "()I", (void*) Native_init },
{ "observe", "(I)V", (void*) Native_observe },
{ "startWatching", "(ILjava/lang/String;I)I", (void*) Native_startWatching },
{ "stopWatching", "(II)V", (void*) Native_stopWatching }

};


int register_os_android_FileWatcher(JNIEnv* env)
{
	jclass clazz;

	clazz = env->FindClass("custom/fileobserver/FileObserver$ObserverThread");

	if (clazz == NULL)
	{
		LOGE("Can't find custom.fileobserver.FileObserver$ObserverThread");
		return -1;
	}

	method_onEvent = env->GetMethodID(clazz, "onEvent","(IIILjava/lang/String;)V");
	if (method_onEvent == NULL)
	{
		LOGE("Can't find FileObserver.onEvent(int, int, String)");
		return -1;
	}

	int res = env->RegisterNatives(clazz, sMethods,
			(sizeof(sMethods) / sizeof(sMethods[0])));
	return res;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jint result = -1;
	jclass native = NULL;

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
	{
		LOGD("ERROR: GetEnv failed1\n");
		return -1;
	}

	result = register_os_android_FileWatcher(env);
	if (result < 0)
	{
		LOGD("ERROR: register_FileWatcher failed1\n");
		return -1;
	}

	return JNI_VERSION_1_4;
}
