function handleEditRequest(editItemCostForm, itemId) {
    const itemCostRow = editItemCostForm.parentElement.parentElement;
    const itemName = itemCostRow.querySelector('td:nth-of-type(1)').firstChild.value;
    const purchaseDate = itemCostRow.querySelector('td:nth-of-type(2)').firstChild.value;
    const category = itemCostRow.querySelector('td:nth-of-type(3)').firstChild.value;
    const itemPrice = itemCostRow.querySelector('td:nth-of-type(4)').firstChild.value;
    const inputFieldsToAdd = {itemId, itemName, purchaseDate, category, itemPrice}

    for(const key in inputFieldsToAdd){
        const hiddenInputField = document.createElement('input');
        hiddenInputField.type = 'hidden';
        hiddenInputField.name = key;
        hiddenInputField.value = inputFieldsToAdd[key]

        editItemCostForm.appendChild(hiddenInputField)
    }

    editItemCostForm.submit()
}

function enableEdit(formEvent, itemId, itemCategories) {
    formEvent.preventDefault();

    const itemCostRow = document.getElementById(itemId);

    const isRowEdited = itemCostRow.getAttribute("data-is-editable")

    if(isRowEdited === 'true'){
        handleEditRequest(formEvent.target, itemId)
    }
    else{
        transformRowToEditable(itemCostRow, itemCategories)
        itemCostRow.setAttribute('data-is-editable', 'true')

        const submitImage = formEvent.target.querySelector('input[type="image"]')
        submitImage.src = '/assets/images/editSaveIcon.png';
        submitImage.alt = 'Save';
        submitImage.title = 'Save';
    }
}

function transformRowToEditable(itemCostRow, itemCategories) {
    const itemNameTableData = itemCostRow.querySelector('td:nth-of-type(1)')
    const purchaseDateTableData = itemCostRow.querySelector('td:nth-of-type(2)')
    const categoryTableData = itemCostRow.querySelector('td:nth-of-type(3)')
    const itemPriceTableData = itemCostRow.querySelector('td:nth-of-type(4)')

    const textInput = createInputElement('text', 'itemName', itemNameTableData.textContent)

    let dateTimeInputValue = ""

    if(!purchaseDateTableData.textContent.includes('Missing')) {
        const convertedDate = new Date(Date.parse(purchaseDateTableData.textContent))
        convertedDate.setMinutes(convertedDate.getMinutes() - convertedDate.getTimezoneOffset())
        dateTimeInputValue = convertedDate.toISOString().slice(0, 16)
    }

    const purchaseDateInput = createInputElement('datetime-local', 'purchaseDate', dateTimeInputValue)
    const categorySelectDropdown = createDropdownSelect('category', itemCategories, categoryTableData.textContent)
    const itemPriceInput = createInputElement('number', 'itemPrice', itemPriceTableData.textContent)
    itemPriceInput.min = '0.0';
    itemPriceInput.step = '0.01';

    itemNameTableData.innerHTML = ""
    purchaseDateTableData.innerHTML = ""
    categoryTableData.innerHTML = ""
    itemPriceTableData.innerHTML = ""

    itemNameTableData.appendChild(textInput)
    purchaseDateTableData.appendChild(purchaseDateInput)
    categoryTableData.appendChild(categorySelectDropdown)
    itemPriceTableData.appendChild(itemPriceInput)
}

function createInputElement(type, name, value) {
    const input = document.createElement('input')
    input.type = type;
    input.name = name
    input.value = value

    return input
}

function createDropdownSelect(name, options, selectedOption) {
    const selectElement = document.createElement('select')
    selectElement.name = name

    const optionElements = []
    for (const option of options) {
        const optionElement = document.createElement('option')
        optionElement.value = option

        if(option === selectedOption){
            optionElement.selected = true
        }

        optionElement.appendChild(document.createTextNode(option))
        optionElements.push(optionElement)
    }

    for(const optionElement of optionElements) {
        selectElement.append(optionElement)
    }

    return selectElement
}