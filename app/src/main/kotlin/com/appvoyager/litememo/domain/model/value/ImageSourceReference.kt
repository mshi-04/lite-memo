package com.appvoyager.litememo.domain.model.value

@JvmInline
value class ImageSourceReference private constructor(val value: String) {

    companion object {
        operator fun invoke(rawValue: String): ImageSourceReference {
            require(rawValue.isNotBlank()) { "ImageSourceReference must not be blank." }
            require(rawValue.isAbsoluteUri()) {
                "ImageSourceReference must be a valid URI."
            }
            return ImageSourceReference(rawValue)
        }
    }
}
