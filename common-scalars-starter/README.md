# 🧩 common-scalars-starter

A **Spring Boot GraphQL starter** that provides several useful scalar types.

---

## ✨ Features

* **`BaseLocalDateTimeScalar`** – Default scalar using
  the [ISO 8601](https://cdn.standards.iteh.ai/samples/70907/5e8d00e639cf4f849462bd63062f4cd8/ISO-8601-1-2019.pdf) local date-time format.
* **`ZuluLocalDateTimeScalar`** – Scalar for Zulu (`UTC`) local date-time format.
* **`Base64Scalar`** – Scalar for Base64-encoded data.
* **`EmailScalar`** (`Email`) – E-mail address: one `@`, no whitespace, a dot in the domain, at most 254 characters.
  Surrounding whitespace is trimmed, case is kept.
* **`PhoneScalar`** (`Phone`) – Phone number in [E.164](https://www.itu.int/rec/T-REC-E.164): `+` and 8 to 15 digits.
  Spaces, dashes, dots and parentheses are dropped, so `+7 (900) 123-45-67` becomes `+79001234567`;
  a number without `+` is rejected.

`Email` and `Phone` check output as well as input, and map to `String` on the JVM: point DGS codegen at it with
`typeMapping.put("Email", "kotlin.String")` and `typeMapping.put("Phone", "kotlin.String")`.

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
