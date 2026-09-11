# kotlinx.serialization сохраняет метаданные сериализаторов через рефлексию имён классов.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kz.chaykin.zakazano.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class kz.chaykin.zakazano.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Типобезопасные маршруты Navigation Compose ищут классы по полному имени.
# Без этого R8 их переименовывает и приложение падает при старте с
# «Cannot find class with name ...» — в debug-сборке это не воспроизводится.
-keep class kz.chaykin.zakazano.ui.navigation.** { *; }

# Перечисления используются и в маршрутах, и в резервной копии через valueOf().
-keep class kz.chaykin.zakazano.model.ItemKind { *; }
-keep class kz.chaykin.zakazano.model.Rating { *; }
-keepclassmembers enum kz.chaykin.zakazano.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
