document.addEventListener('DOMContentLoaded',function(){
    const signupForm = document.querySelector('section');
    signupForm.style.opacity = 0;


    setTimeout(() => {
        signupForm.style.transition = 'opacity 1s ease-in-out';
        signupForm.style.opacity = 1;
    },500);


    const signupButton = document.querySelector('button');
    signupButton.addEventListener('click',function(){
        const nameInput  = document.querySelector('input[id="nombre"]');
        const apellidoInput = document.querySelector('input[id="apellidos"]');
        const emailInput = document.querySelector('input[id="email"]');
        const passwordInput = document.querySelector('input[id="contraseña"]');
        const confirmPasswordInput = document.querySelector('input[id="confirmar-con"]');


        const isValid = emailInput.checkValidity() && passwordInput.checkValidity() && confirmPasswordInput.checkValidity();
        
        if(isValid) {
            signupForm.classList.add('shake');

            setTimeout(()=>{
                signupForm.classList.remove('shake');
            },1000);

        }
    });


});