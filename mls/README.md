# BouncyCastle MLS (Koe fork)

This is a fork of [BouncyCastle's](https://www.bouncycastle.org/) MLS library.

### Personal notes / changes

- BC MLS lib seems to be a direct port of Cisco's MLS++ library (so what libdave uses) and most things map 1:1.
    - https://github.com/bcgit/bc-java/issues/1317
- When I noticed first roadblocks and realized I have to modify the source code, I first tried to port it to Tink.
    - However Tink seems to be pretty badly documented. I had to heavily rely on the source code.
    - It's missing certain HKDF functionality, so I had to either reimplement it using code from `internal` package
      (which does not sound future-proof) or completely reinvent the wheel.
    - I decided to give up and just stick with BouncyCastle and maybe replace Tink with it in Koe to reduce amount of
      dependencies that do the same thing.
- I've removed dependency on gRPC and Protobuf
    - Why does BC MLS ship with these?
    - It's only purpose seems to be interop testing apparently: https://github.com/bcgit/bc-java/pull/1565
- `Capabilities` class doesn't expose any of it's fields, so I couldn't change extensions and supported suites to what
  DAVE expects without parsing a serialized message...
- `Group` was missing an equivalent of `State::unwrap` method.
- `Welcome` has no clean way to access `secrets`, `EncryptedGroupSecrets` were private.
- The library has many methods that only accept serialized messages and even does redundant re-serialization internally
  in some places :(
    - most notably key handling? (ctrl+f `serializePrivateKey`)
- Added `TreeKEMPublicKey.allLeaves` method.
- Added `equals` to `ExternalSender`.
- Split `HashRatchet` out of `GroupKeySet`.

### Possible replacements

- https://github.com/Traderjoe95/mls-kotlin (written in Kotlin, but requires Java 21)
- write our own?
