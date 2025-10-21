# 🧩 common-scalars-starter

A **Spring Boot GraphQL starter** that provides several useful scalar types.

---

## ✨ Features

* **`BaseLocalDateTimeScalar`** – Default scalar using
  the [ISO 8601](https://cdn.standards.iteh.ai/samples/70907/5e8d00e639cf4f849462bd63062f4cd8/ISO-8601-1-2019.pdf) local date-time format.
* **`ZuluLocalDateTimeScalar`** – Scalar for Zulu (`UTC`) local date-time format.
* **`Base64Scalar`** – Scalar for Base64-encoded data.

---

## ⚙️ Installation

Add the dependency to your **Gradle (Kotlin DSL)** configuration:

```kotlin
dependencies {
    implementation("io.github.denis-markushin:common-scalars-starter:<latest-version>")
}
```

> 💡 Replace `<latest-version>` with the newest version available on [Maven Central](https://search.maven.org/).

---

## 🚀 Usage

To enable support for **Zulu (`UTC`) format** in `LocalDateTime` serialization, add the following to your `application.yml`:

```yaml
dgs:
  graphql:
    dema:
      scalars:
        enabled: true
        localdatetime:
          format: zulu
```

---

## 📄 License

This project is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).
