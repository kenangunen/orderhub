# ADR-0004: Servis sınırları ve veri sahipliği

- Durum: Kabul edildi
- Tarih: 2026-11-15
- Ekler: 2026-11-16 (stok opt-in), 2026-11-18 (stok alt kaynağı)

## Bağlam

OrderHub şu an tek servisten (product-service) oluşuyor. Yeni ihtiyaçlar:
ürün detayında stok adedini göstermek ve ileride sipariş almak.

Stok adedini `Product`'a bir alan olarak eklemek en kısa yol. Ama stok, üründen
çok farklı bir hızda ve farklı nedenlerle değişir: ürün bilgisi ayda bir
güncellenir, stok her siparişte. Farklı kurallar, farklı yük ve ileride farklı
ekipler demek. Bu yüzden sistemi iş alanlarına (bounded context) göre bölüyoruz.

## Karar

### Bounded context'ler

| Context | Servis | Sorumluluk |
|---|---|---|
| Catalog | product-service | Ürünün ne olduğu: ad, açıklama, fiyat, kategori, aktiflik |
| Inventory | inventory-service | Üründen kaç tane olduğu: kullanılabilir ve rezerve adet |
| Ordering | order-service | Kimin neyi ne fiyata sipariş ettiği, siparişin durumu |

### Veri sahipliği

| Veri | Sahibi | Not |
|---|---|---|
| Ürün adı, açıklama, kategori, fiyat | product-service | Tek doğru kaynak |
| Kullanılabilir / rezerve adet | inventory-service | product-service bu veriyi tutmaz, sorar |
| Sipariş, sipariş satırı | order-service | — |
| Sipariş satırındaki birim fiyat | order-service (**snapshot**) | Sipariş anında product-service'ten alınıp satıra kopyalanır. Fiyat sonradan değişse de geçmiş siparişin tutarı değişmez. İstemci fiyat göndermez. |

Bir verinin tek sahibi vardır; diğer servisler o veriyi sahibinden API ile ister
ya da gerektiğinde kopyasını (snapshot) kendi amacı için saklar.

### Ortak anahtar: SKU

Servisler ürünü **SKU** ile tanır. UUID, product-service'in kendi iç kimliğidir:
onu product-service üretir ve sadece o bilir. SKU ise iş dünyasının anahtarıdır:
depo, sipariş ve katalog aynı SKU'yu kullanır, değişmez (`Product`'ta `final`).

### İletişim

- **Şimdi, senkron (REST + RestClient):** Okuma ihtiyaçları anlık cevap
  gerektirir.
    - product-service → inventory-service: ürün detayında stok adedi (B10-B)
    - order-service → product-service: sipariş anında fiyat, okumada ürün adı (B10-E)
- **İleride, asenkron (Kafka):** Durum değiştiren servisler arası akışlar.
  Sipariş verilince stoğun rezerve edilmesi `OrderPlaced` event'i ile yapılır
  (choreography saga, Hafta 3). Böylece sipariş anında inventory-service'in
  ayakta olması gerekmez.

### Veritabanı: database-per-service

Her servisin kendi veritabanı vardır. Bir servis başka servisin tablosunu
okuyamaz ve yazamaz; veriye sadece o servisin API'si veya event'leri üzerinden
erişilir. Aksi halde tablo yapısı servisler arası gizli bir sözleşmeye dönüşür
ve servisler bağımsız değiştirilemez (distributed monolith).

## Değerlendirilen alternatifler

1. **Stok adedini `Product`'a alan olarak eklemek.** En basit yol. Reddedildi:
   ürün ve stok farklı hızda değişir. Her siparişte ürün kaydı kilitlenir,
   katalog okumaları stok yazmalarıyla yarışır, stok kuralları (rezervasyon,
   negatif olamaz) Catalog modeline karışır.
2. **Servisleri ayırıp tek ortak veritabanı kullanmak.** Geçişi kolaylaştırır.
   Reddedildi: servisler şema üzerinden birbirine bağlanır; bir tablodaki
   değişiklik tüm servisleri birlikte deploy etmeyi gerektirir.
3. **UUID'yi ortak anahtar yapmak.** Reddedildi: diğer servisler ürünü önce
   product-service'e sormadan tanıyamaz; iş tarafının kullandığı kimlik SKU'dur.

## Sonuçlar

**Artılar**
- Her servis bağımsız geliştirilir, test edilir, deploy edilir.
- Stok tarafı yük altında kalırsa sadece inventory-service ölçeklenir.
- Her verinin sahibi net; "bu alanı kim değiştirebilir" sorusunun tek cevabı var.

**Bedeller ve nerede ele alınıyor**
- **Ağ çağrısı ve latency:** Ürün detayı artık iki servise bağlı.
  → B10-C: stok bilgisi opt-in (`?showInventory`), ürün bilgisi cache'lenir.
- **Kısmi hata:** inventory-service kapalıysa ürün detayı ne olacak?
  → B10-B: timeout + tolerans (stok `null`, ürün yine 200).
  → Hafta 3: Resilience4j circuit breaker.
- **Tutarlılık:** Sipariş ve stok rezervasyonu iki ayrı veritabanında; tek
  transaction yok. → Hafta 3: saga + idempotent consumer, eventual consistency.
- **Operasyon yükü:** Üç servis, üç port, üç veritabanı.
  → B10-A: port tablosu; B10-D: ortak parent pom; ileride Docker Compose.

## Ek (2026-11-16): Stok bilgisi opt-in

**Karar:** Ürün detayı stok bilgisini sadece `?showInventory=true` ile içerir; varsayılan `false`.
Stok istenmediğinde inventory-service hiç çağrılmaz.

- Service'te iki ayrı metot: `getById` (stoksuz) ve `getByIdWithInventory` (stoklu).
  Boolean parametreli tek metot (flag argument) yerine, her metot tek iş yapar ve adı ne yaptığını söyler.
- `quantityOnHand` değeri yoksa JSON'da hiç görünmez (`@JsonInclude(NON_NULL)`, sadece bu alan).
- Bilinen bedel: Stok istendi ama inventory'ye ulaşılamadıysa da alan görünmez; istemci
  "bilinmiyor" ile "istenmedi"yi ayırt edemez. Kesin bilgi isteyen istemciler için
  `GET /products/{id}/inventory` (inventory kapalıysa 503) eklenecek.
- Reddedilen: ayrı bir `stockStatus` alanı. Bugün bu ayrıma ihtiyacı olan istemci yok,
  sözleşme gereksiz büyür.

## Ek (2026-11-18): Stok alt kaynağı ve inventory arızasında davranış

**Bağlam:** 16 Kasım ekinde sözü verilen alt kaynak eklendi. Stok bilgisine artık iki yoldan ulaşılıyor:
- `GET /api/v1/products/{productId}?showInventory=true` → ürün detayı + `quantityOnHand`
- `GET /api/v1/products/{productId}/inventory` → sadece stok: `ProductInventoryResponse(productId, sku, availableQty)`

**Karar:** İkisi birlikte kalır, ama hata durumlarında **farklı** davranırlar:

| Durum | `?showInventory=true` | `/{productId}/inventory` |
|---|---|---|
| Stok var | 200 + `quantityOnHand` | 200 + `availableQty` |
| Ürün yok | 404 `Product Not Found` | 404 `Product Not Found` |
| Stok kaydı yok | 200, `quantityOnHand` yok | 404 `Inventory Not Found` |
| inventory-service kapalı | 200, `quantityOnHand` yok (graceful degradation) | 503 `Inventory Unavailable` + `Retry-After: 30` |

**Gerekçe:** Detay endpoint'inde stok bir **ek bilgidir**; yokluğu ürünü göstermeye engel değil.
Alt kaynakta stok endpoint'in **tek işidir**; yokken 200 dönmek "başarılı ama boş" demek olur.
`InventoryClient` her iki yolda aynı `RestClientException`'ı fırlatır; **yorumu çağıran service metodu yapar**:
`getByIdWithInventory` hatayı yutar (`warn` + `null`), `getInventory` onu `InventoryUnavailableException`'a
çevirir. Asıl hata `cause` olarak taşınır ve log'a yazılır, istemciye sızmaz.

- `availableQty` primitive `int`: 200 dönülüyorsa adet her zaman vardır; "yok" ve "bilinmiyor"
  durumları status ile ifade edilir.
- "Kayıt yok" ile "stok 0" farklıdır; kayıt yokken 0 dönülmez.
- İki 404 aynı status'u, farklı `title`'ı kullanır; istemci ürünün mü stok kaydının mı olmadığını ayırt eder.

**Değerlendirilen alternatifler:**
- `?showInventory`'yi kaldırıp tek yol bırakmak: detay ekranı iki çağrı yapmak zorunda kalır.
- Alt kaynakta da 200 + `null`: istemci her cevapta "null mı, sayı mı" kontrolü yapar; 503'ün
  "geçici, tekrar dene" anlamı (retry, load balancer, circuit breaker) kaybolur.

**Sonuç:** Hafta 3'te Resilience4j circuit breaker açıkken de aynı exception ve aynı 503 kullanılacak.