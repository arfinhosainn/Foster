# Suggested diff — add the web "Delete Account" link to Settings

Play requires an in-app deletion path **and** a publicly visible deletion route; linking the
deletion web page from Settings makes both visible in one place and gives reviewers an
obvious path. The in-app deletion itself already exists (`DeleteAccountBottomSheet`).

Also add the string resource to
`home/src/commonMain/composeResources/values/strings.xml`:

```xml
<string name="settings_delete_account_web">Delete your account (web)</string>
```

(and the translated equivalents in other locale `values-*` folders).

```diff
--- a/home/src/commonMain/kotlin/app/usefoster/home/presentation/settings/SettingScreen.kt
+++ b/home/src/commonMain/kotlin/app/usefoster/home/presentation/settings/SettingScreen.kt
@@ -88,6 +88,7 @@
 /** Public legal-document URLs (hosted on the Foster Framer site). */
 private const val PRIVACY_POLICY_URL = "https://fosterapp.framer.website/privacy"
 private const val TERMS_URL = "https://fosterapp.framer.website/terms"
+private const val DELETE_ACCOUNT_URL = "https://fosterapp.framer.website/delete-account"
 private const val SUPPORT_EMAIL_URI = "mailto:programmingwitharfin@gmail.com"
@@ -311,6 +312,24 @@
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_POLICY_URL) },
                 ) {
 
                     Icon(
                         imageVector = vectorResource(Res.drawable.ic_privacy),
                         contentDescription = "",
                         tint = Color.Unspecified
                     )
                     Spacer(Modifier.width(5.dp))
 
                     Text(
                         stringResource(Res.string.settings_privacy),
                         fontSize = 14.sp,
                         fontWeight = FontWeight.Medium,
                         color = FosterTheme.colors.text.secondary
                     )
 
                     Spacer(Modifier.height(34.dp))
 
                 }
 
+                Spacer(Modifier.height(3.dp))
+
+                Row(
+                    verticalAlignment = Alignment.CenterVertically,
+                    modifier = Modifier.clickable { uriHandler.openUri(DELETE_ACCOUNT_URL) },
+                ) {
+                    Icon(
+                        imageVector = vectorResource(Res.drawable.ic_trashbin),
+                        contentDescription = "",
+                        tint = Color.Unspecified
+                    )
+                    Spacer(Modifier.width(5.dp))
+
+                    Text(
+                        stringResource(Res.string.settings_delete_account_web),
+                        fontSize = 14.sp,
+                        fontWeight = FontWeight.Medium,
+                        color = FosterTheme.colors.text.secondary
+                    )
+                }
+
             }
```

Apply manually (or tell me to apply it) — note the existing file also has a small bug in the
privacy row (`Modifier.height(34.dp)` inside the row uses the wrong `modifier` receiver);
fixing that while you are in there is recommended.
