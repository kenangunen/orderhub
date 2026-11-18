![orderhub CI](https://github.com/kenangunen/orderhub/actions/workflows/ci.yml/badge.svg)

# OrderHub

Java 21 + Spring Boot 4 ile mini e-ticaret sipariş platformu (mikroservis mimarisi).

## Servisler

| Servis | Port | Sorumluluk |
|---|---|---|
| product-service | 8081 | Katalog: ürün adı, açıklama, fiyat, kategori |
| inventory-service | 8082 | Stok: kullanılabilir ve rezerve adet |
| order-service | 8083 | Sipariş (planlandı) |

Servisler birbirini HTTP ile çağırır; ortak kod paylaşmaz (bkz. `docs/adr/0001-mono-repo.md`, `docs/adr/0004-service-boundaries.md`).

## Gereksinimler

- JDK 21 veya üstü (build başında Maven Enforcer kontrol eder)
- Maven kurulu olmak zorunda değil: repo kökündeki `./mvnw` (Maven Wrapper) doğru sürümü indirir

## Build ve test

Tüm komutlar **repo kökünden** çalıştırılır. Kök `pom.xml` hem tüm servisleri build eder (aggregator)
hem de ortak ayarları (Java sürümü, Lombok, Enforcer, Spring Cloud sürümleri) sağlar (parent).

| İş | Komut |
|---|---|
| Tüm servislerin testleri | `./mvnw test` |
| Sıfırdan temiz build + test | `./mvnw clean verify` |
| Tek servisin testleri | `./mvnw -pl inventory-service test` |

`-pl` (project list): komutu sadece belirtilen modülde çalıştırır.

## Çalıştırma

Servisler **`dev` profiliyle** çalıştırılmalıdır; bu profilde açılışta örnek veri yüklenir.
Profil `application.yaml`'a yazılmaz, çalıştırırken verilir.

```bash
./mvnw -pl product-service spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw -pl inventory-service spring-boot:run -Dspring-boot.run.profiles=dev
```

(Her biri ayrı bir terminalde.)

**Örnek veri:** iki serviste de aynı SKU'lar yüklenir.

| SKU | Stok |
|---|---|
| KLV-0001 | 50 |
| MSE-0001 | 0 |
| MNT-0001 | 12 |

### IntelliJ IDEA

Kök `pom.xml`'i proje olarak açın. Hazır çalıştırma ayarları `.run/` klasöründedir ve otomatik görünür:

- `product-service (dev)`, `inventory-service (dev)`: tek servis, `dev` profiliyle
- `orderhub (all services)`: tüm servisleri birlikte başlatır (Compound)

## API'yi denemek

Postman koleksiyonu: `postman/orderhub.postman_collection.json` (Postman'de adı `orderHub`).

- Servisler yeniden başlatıldığında ürün id'leri değişir (veri bellekte tutulur). Önce **ürün listesi**
  isteğini gönderin; `{{productId}}` değişkeni KLV-0001'in id'siyle otomatik dolar.
- Hata durumları her servis klasöründeki `errors` alt klasöründedir.

## Mimari kararlar

`docs/adr/` klasöründe:

| ADR | Konu |
|---|---|
| 0001 | Mono-repo, kök pom (aggregator + parent) |
| 0004 | Servis sınırları ve veri sahipliği, stok bilgisine erişim |
| 0005 | Ürün okumaları için cache (Caffeine) |