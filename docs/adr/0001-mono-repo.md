# ADR-0001: Mono-repo kullanımı

- Durum: Kabul edildi
- Tarih: 2026-11-06
- Ekler: 2026-11-18 (kök pom: aggregator + parent, CI kökten)

## Bağlam

OrderHub; product, order ve inventory servisleri ile gateway ve discovery
bileşenlerinden oluşacak bir mikroservis projesi. Projeyi tek kişi geliştiriyor.
Servisler aynı anda değişecek (ör. sipariş akışı order + inventory'yi birlikte
etkiler) ve portföy olarak tek bir linkten incelenebilir olması gerekiyor.

## Karar

Tüm servisler tek bir Git reposunda, her biri kendi klasöründe bağımsız
bir Maven projesi olarak tutulacak (`orderhub/product-service`, `orderhub/order-service` ...).
Servisler kod paylaşmaz; birbirleriyle sadece API (HTTP/Kafka) üzerinden konuşur.

## Alternatifler

- **Poly-repo (her servis ayrı repo):** Servis sahipliği ve bağımsız versiyonlama
  net olur; büyük ve birden fazla takımın çalıştığı yapılarda tercih edilir.
  Tek geliştirici için ise birden fazla servisi etkileyen değişiklik birden
  fazla PR'a bölünür, CI/README/Docker Compose ayarları çoğalır.
- **Maven multi-module (parent pom altında modüller):** Ortak bağımlılık yönetimi
  kolaylaşır, ancak servisler build seviyesinde birbirine bağlanır ve
  "bağımsız deploy edilebilir servis" fikrini zayıflatır.

## Sonuçlar

**Artılar**
- Birden fazla servisi etkileyen değişiklik tek commit/PR ile yapılır.
- Tek README, tek Docker Compose, tek CI ile tüm sistem ayağa kalkar.
- Portföy olarak tek linkten incelenebilir.

**Eksiler / riskler**
- Servisler arası sınırlar disiplinle korunmalı; bir servisin diğerinin
  kodunu import etmesi mümkün ama yasak.
- CI'da her commit'te tüm servisleri build etmemek için path bazlı tetikleme gerekir.
- Proje büyüyüp birden fazla takıma dağılırsa poly-repo'ya geçiş düşünülmeli.

## Ek (2026-11-18): Kök pom — aggregator + parent

**Bağlam:** İkinci servis (inventory-service) eklendi, üçüncüsü (order-service) geliyor. Her servisin
pom'unda Java/Boot sürümü ve plugin ayarları tekrar ediyor; testler her servis klasöründe ayrı çalıştırılıyor;
CI sadece product-service'i test ediyor.

**Karar:**
- Repo kökünde tek bir `pom.xml` (`com.orderhub:orderhub-root`, `packaging pom`) hem **aggregator**
  (`<modules>`: tek komutla tüm servislerin build'i) hem **parent** (servisler ayarları miras alır).
- Kök pom'un parent'ı `spring-boot-starter-parent`. **Tüm servisler aynı Spring Boot sürümünü kullanır**;
  yükseltme tek yerden yapılır. Bir servisin farklı sürüme ihtiyacı olursa kök parent'tan çıkar ve bu
  istisna ayrıca kayda geçirilir.
- Kökte tek yerden yönetilenler: `java.version`, Spring Cloud sürüm tablosu (`spring-cloud-dependencies`
  BOM, `import` scope), Lombok annotation processor ayarı, Maven Enforcer (Java ≥ 21, Maven ≥ 3.9,
  tekrar eden bağımlılık yasağı).
- Kökteki `<dependencies>` (tüm servislere zorla girer) **minimum**: sadece `spring-boot-starter-test`
  (test) ve Lombok. Diğer bağımlılıklar servislerde kalır.
- Tek Maven Wrapper repo kökünde; servis klasörlerinde wrapper yok. Komutlar kökten çalıştırılır
  (tek servis için `-pl <servis>`).
- CI (`.github/workflows/ci.yml`) kökten `./mvnw -B verify` çalıştırır; tüm servisler her build'de test edilir.
  Tetikleme `paths` yerine `paths-ignore` ile: sadece `docs/`, `postman/`, `.run/` ve markdown değişiklikleri
  CI'ı tetiklemez; yeni bir servis eklendiğinde ayrıca listeye eklenmesi gerekmez.

**Orijinal karar ile ilişki:** Yukarıdaki "Alternatifler" bölümü multi-module yapıyı "servisler build
seviyesinde birbirine bağlanır" diye reddediyordu. Bu ek o kararı tersine çevirmez:
- Servisler hâlâ **ayrı jar** üretir, ayrı çalışır ve birbirinin kodunu import etmez.
- Parent/BOM **kod paylaşmaz**; paylaşılan tek şey build ayarı ve kütüphane sürümleridir.
- Kabul edilen bağlılık bilinçlidir: tüm servisler aynı Boot ve Java sürümünde ilerler.

Orijinal "Sonuçlar"daki "path bazlı tetikleme gerekir" riski yeniden değerlendirildi: tek geliştiricili ve az
servisli bir projede her kod değişikliğinde tüm servisleri test etmek (bugün ~10 sn) kabul edilebilir bir
maliyet; kök pom değişikliklerinin tüm servisleri etkilemesi de bunu gerektiriyor.

**Versiyonlama:** Kök pom ve servisler aynı commit'te build edildiği için parent ayrıca release edilmez;
sürüm `0.0.1-SNAPSHOT` olarak kalır. Çok repolu bir yapıda olsaydı parent (ya da BOM) ayrı bir repoda tutulur,
semantik versiyonlama ile release edilir ve her servis onu kendi hızında, sabit bir sürümle kullanırdı.
Production'a SNAPSHOT bağımlılıkla çıkılmaz: SNAPSHOT'ın içeriği değişebildiği için build tekrarlanabilir olmaz.

**Değerlendirilen alternatifler:**
- Ayrı `orderhub-parent` modülü (kök sadece aggregator): parent'ı ileride ayrı repoda release etmeyi
  kolaylaştırır; mono-repo'da her şey aynı commit'te build edildiği için bugün kazancı yok.
- Ortak starter'ları (web, validation, actuator) kökte toplamak: web olmayan bir servis (ör. sadece Kafka
  dinleyen bir consumer) gereksiz yere Tomcat taşır.
- Ayrı repoda release edilen BOM (kursun yolu): çok repolu yapıda gerekir; mono-repo'da gereksiz.