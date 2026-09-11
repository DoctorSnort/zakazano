# kotlinx.serialization сохраняет метаданные сериализаторов через рефлексию имён классов.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kz.chaykin.zakazano.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class kz.chaykin.zakazano.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
