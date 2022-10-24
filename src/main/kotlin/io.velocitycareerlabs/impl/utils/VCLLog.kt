/**
 * Created by Michael Avoyan on 3/12/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.utils

import io.velocitycareerlabs.impl.GlobalConfig
import io.velocitycareerlabs.impl.GlobalConfig.LogTagPrefix
import mu.KotlinLogging

internal object VCLLog {
    private val logger = KotlinLogging.logger {}

    /**
     * Send a [.VERBOSE] log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    fun v(tag: String?, msg: String): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Verbose, LogTagPrefix + tag, msg) else -1
    }

    /**
     * Send a [.VERBOSE] log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun v(tag: String?, msg: String?, tr: Throwable?): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Verbose, LogTagPrefix + tag, msg, tr) else -1
    }

    /**
     * Send a [.DEBUG] log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    fun d(tag: String?, msg: String): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Debug, LogTagPrefix + tag, msg) else -1
    }

    /**
     * Send a [.DEBUG] log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun d(tag: String?, msg: String?, tr: Throwable?): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Debug, LogTagPrefix + tag, msg, tr) else -1
    }

    /**
     * Send an [.INFO] log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    fun i(tag: String?, msg: String): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Info, LogTagPrefix + tag, msg) else -1
    }

    /**
     * Send a [.INFO] log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun i(tag: String?, msg: String?, tr: Throwable?): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Info, LogTagPrefix + tag, msg, tr) else -1
    }

    /**
     * Send a [.WARN] log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    fun w(tag: String?, msg: String): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Warning, LogTagPrefix + tag, msg) else -1
    }

    /**
     * Send a [.WARN] log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun w(tag: String?, msg: String?, tr: Throwable?): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Warning, LogTagPrefix + tag, msg, tr) else -1
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun w(tag: String?, tr: Throwable?): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Warning, LogTagPrefix + tag, null, tr) else -1
    }

    /**
     * Send an [.ERROR] log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    fun e(tag: String?, msg: String): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Error, LogTagPrefix + tag, msg) else -1
    }

    /**
     * Send a [.ERROR] log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    fun e(tag: String?, msg: String?, tr: Throwable?): Int {
        return if (GlobalConfig.IsLoggerOn) log(LogLevel.Error, LogTagPrefix + tag, msg, tr) else -1
    }

    private fun log(level: LogLevel, tag: String, msg: String?, throwable: Throwable? = null): Int {
        val log = "$tag: ${msg ?: ""} ${throwable ?: ""}"
        when (level) {
            LogLevel.Verbose -> logger.info(log)
            LogLevel.Info -> logger.info(log)
            LogLevel.Debug -> logger.debug(log)
            LogLevel.Warning -> logger.warn(log)
            LogLevel.Error -> logger.error(log)
        }
        return 0
    }

    private enum class LogLevel {
        Verbose,
        Info,
        Debug,
        Warning,
        Error
    }
}
