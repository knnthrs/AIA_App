# Keep attributes for Firebase annotations and reflections
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes InnerClasses

# Keep class members for Firebase (general rule, often good to have)
-keepnames class com.google.firebase.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Keep your model classes and their members that Firebase will access
# This rule keeps all public members and the default constructor of classes in your models package
-keep public class com.example.signuploginrealtime.models.** {
    public <init>(); # Keep default constructor
    public *;        # Keep all public fields and methods (getters/setters)
}

# Specifically for ExerciseInfo, ensure fields are kept if they are not public or if you want to be extra safe
# This is often redundant if the rule above is used and fields/getters are public, but can be more explicit.
-keepclassmembers class com.example.signuploginrealtime.models.ExerciseInfo {
    java.lang.String id;
    java.lang.String name;
    java.lang.String gifUrl;
    java.util.List bodyParts;
    java.util.List equipments;
    java.util.List targetMuscles;
    java.util.List secondaryMuscles;
    java.util.List instructions;
    # Add any other fields from ExerciseInfo that Firebase needs to populate
}

# If you use @PropertyName annotations from Firebase, ensure they are kept
-keep class com.google.firebase.database.PropertyName { *; }
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
}
