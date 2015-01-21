# Material Elements

This is a library containing useful Material-Design elements for Android. If we find a good library for a special purpose we are using it.

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc/generate-toc again -->
**Table of Contents**

- [Material Elements](#material-elements)
    - [Views](#views)
        - [Floating Action Button](#floating-action-button)

<!-- markdown-toc end -->

## Gradle dependency

```TODO```


## Views

### Floating Action Button

```TODO: Image```

We are using [shamanland/floating-action-button](https://github.com/shamanland/floating-action-button) as the underlying library.

Just add the following piece of code to the bottom of your `FrameLayout`:

```xml
    <de.azapps.material_elements.views.FloatingActionButton
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"        <!-- place it wherever you want -->
        android:layout_marginBottom="16dp"
        android:layout_marginEnd="16dp"
        android:src="@drawable/ic_plus_white_24dp" <!-- use another image -->
        app:floatingActionButtonSize="normal"      <!-- normal or small -->
        app:floatingActionButtonColor="?colorAccent"
        />
```
