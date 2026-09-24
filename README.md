# PDF Extractor

Applicazione web con front-end HTML, CSS e JavaScript e backend REST Java Spring Boot per validare e salvare PDF in una directory locale. L'estrazione del testo non e' ancora implementata.

## Architettura

Il [documento di architettura](.github/modernize/assessment/engines/facts/architecture-diagram.md) descrive componenti, tecnologie e versioni, diagrammi, API REST, flusso di upload, configurazione e limiti di sicurezza.

## Avvio

Il backend richiede Java 17 e Maven 3.9. Lo script usa la JDK OpenLogic
in `%USERPROFILE%\openlogic-openjdk-17.0.9+9-windows-x64`, senza modificare
le variabili di ambiente globali.

```bat
scripts\dev.cmd run
scripts\dev.cmd test
scripts\dev.cmd package
```

Per il caricamento effettivo, aprire l'applicazione tramite il server HTTP,
non direttamente dal file `index.html`. L'indirizzo predefinito e' <http://localhost:8080>.

## Struttura

```text
pdf-extractor/
  index.html          Pagina principale
  src/
    styles.css        Stili responsive e stati di feedback
    app.js            Controlli client e upload REST
  assets/
    upload.svg        Icona del pulsante
  tests/
    index.html        Test eseguibili nel browser
    validation.js     Casi di controllo e interazione
  backend/
    pom.xml           Dipendenze e build Maven
    src/main/         Codice Java e configurazione Spring Boot
    src/test/         Test backend
  scripts/
    dev.cmd           Avvio, test e packaging con Java 17
  uploads/            PDF salvati localmente
```

## Comportamento

- Il pulsante apre il selettore nativo di file, filtrato per PDF.
- Il browser controlla estensione `.pdf`, senza distinzione tra maiuscole e minuscole, e dimensione non nulla fino a 10 MiB.
- Le altre estensioni producono un messaggio rosso. Nel selettore si puo' scegliere "Tutti i file" per provarle.
- Annullare la selezione mantiene l'ultimo esito. E' possibile riselezionare lo stesso file.
- Il file viene inviato a `POST /api/files`; il backend controlla anche il contenuto con PDFBox e rifiuta PDF cifrati o senza pagine.
- Solo dopo validazione il documento viene salvato in `uploads/` con nome UUID. La directory e' configurabile con `UPLOAD_DIR`.
- Il messaggio verde conferma il salvataggio; i controlli falliti e gli errori di caricamento producono un messaggio rosso.
- La validazione non sostituisce una scansione antivirus. L'estrazione del testo non e' ancora implementata.

Il font Manrope viene richiesto a Google Fonts, senza invio del file selezionato. In assenza di rete viene usato il font sans-serif del browser. L'icona upload e' basata su Lucide.

## Test

Aprire `tests/index.html` nel browser: la pagina riporta l'esito dei controlli automatici. Verificare anche la pagina principale su desktop e mobile e l'apertura del selettore con tastiera.
