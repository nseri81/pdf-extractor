# PDF Extractor

Prima implementazione solo front-end, realizzata con HTML, CSS e JavaScript.

## Avvio

Aprire `index.html` in un browser moderno. Non servono installazioni, build o server.

## Struttura

```text
pdf-extractor/
  index.html          Pagina principale
  src/
    styles.css        Stili responsive e stati di feedback
    app.js            Selezione e controllo dell'estensione
  assets/
    upload.svg        Icona del pulsante
  tests/
    index.html        Test eseguibili nel browser
    validation.js     Casi di controllo e interazione
```

## Comportamento

- Il pulsante apre il selettore nativo di file, filtrato per PDF.
- Un nome con estensione `.pdf`, senza distinzione tra maiuscole e minuscole, produce un messaggio verde.
- Le altre estensioni producono un messaggio rosso. Nel selettore si puo' scegliere "Tutti i file" per provarle.
- Annullare la selezione mantiene l'ultimo esito. E' possibile riselezionare lo stesso file.
- Nessun file viene letto, salvato o trasmesso. Nessun backend e nessuna estrazione sono implementati.
- La verifica riguarda esclusivamente il nome: rinominare un file in `.pdf` basta a superarla. Non e' una validazione del contenuto o di sicurezza.

Il font Manrope viene richiesto a Google Fonts, senza invio del file selezionato. In assenza di rete viene usato il font sans-serif del browser. L'icona upload e' basata su Lucide.

## Test

Aprire `tests/index.html` nel browser: la pagina riporta l'esito dei controlli automatici. Verificare anche la pagina principale su desktop e mobile e l'apertura del selettore con tastiera.