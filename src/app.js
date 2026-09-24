const selectButton = document.querySelector("#select-file");
const fileInput = document.querySelector("#file-input");
const feedback = document.querySelector("#feedback");

function hasPdfExtension(filename) {
  return filename.length > 4 && /\.pdf$/i.test(filename);
}

selectButton.addEventListener("click", () => {
  fileInput.click();
});

fileInput.addEventListener("change", () => {
  const file = fileInput.files[0];

  if (!file) return;

  const valid = hasPdfExtension(file.name);
  feedback.dataset.state = valid ? "success" : "error";
  feedback.textContent = valid
    ? "File PDF selezionato correttamente."
    : "Formato non valido. Seleziona un file con estensione .pdf.";

  fileInput.value = "";
});