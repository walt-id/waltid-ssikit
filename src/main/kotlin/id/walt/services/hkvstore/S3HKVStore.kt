package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceConfiguration
import io.minio.*
import io.minio.messages.Tags
import mu.KotlinLogging
import java.nio.file.Path

data class S3StoreConfig(
  val endpoint: String,
  val bucket: String,
  val access_key: String? = null,
  val secret_key: String? = null
) : ServiceConfiguration {

}

class S3HKVStore(configPath: String) : HKVStoreService() {
  override lateinit var configuration: S3StoreConfig
  var _s3Client: MinioClient? = null
  val s3Client: MinioClient
    get() {
      if(_s3Client == null) {
        val s3Builder = MinioClient.builder().endpoint(configuration.endpoint)
        if(!configuration.access_key.isNullOrEmpty()) {
          s3Builder.credentials(configuration.access_key ?: "", configuration.secret_key ?: "")
        }
        _s3Client = s3Builder.build()
        if(!_s3Client!!.bucketExists(BucketExistsArgs.builder().bucket(configuration.bucket).build())) {
          logger.info { "Creating S3 bucket ${configuration.bucket}" }
          _s3Client!!.makeBucket(MakeBucketArgs.builder().bucket(configuration.bucket).build())
        } else {
          logger.info { "S3 bucket ${configuration.bucket} already exists" }
        }
      }
      return _s3Client!!
    }


  constructor(config: S3StoreConfig) : this("") {
    configuration = config
  }

  private val logger = KotlinLogging.logger {}

  init {
    if (configPath.isNotEmpty()) configuration = fromConfiguration(configPath)
  }

  override fun put(key: HKVKey, value: ByteArray) {
    value.inputStream().use {
      s3Client.putObject(
        PutObjectArgs.builder()
          .bucket(configuration.bucket)
          .`object`(key.toString())
          .extraHeaders(mapOf("ETag" to key.toString()))
          .stream(it, value.size.toLong(), -1)
          .build()
      )
    }
  }

  override fun getAsByteArray(key: HKVKey): ByteArray? {
    return s3Client.listObjects(ListObjectsArgs.builder()
      .bucket(configuration.bucket)
      .prefix(key.toString())
      .recursive(false).build())
      .filter { it.get().objectName() == key.toString().trim('/') }
      .firstOrNull()?.let {
        s3Client.getObject(GetObjectArgs.builder()
          .bucket(configuration.bucket)
          .`object`(key.toString())
          .build()).use {
          it.readAllBytes()
        }
      }
  }

  override fun listChildKeys(parent: HKVKey, recursive: Boolean): Set<HKVKey> {
    return s3Client.listObjects(ListObjectsArgs.builder()
      .bucket(configuration.bucket)
      .prefix("$parent/")
      .recursive(recursive)
      .build()).filter {
        !it.get().isDir
    }.map {
        HKVKey.fromString(it.get().objectName())
    }.toSet()
  }

  override fun delete(key: HKVKey, recursive: Boolean): Boolean {
    s3Client.removeObject(RemoveObjectArgs.builder()
      .bucket(configuration.bucket)
      .`object`(key.toString()).build())
    return true
  }
}
