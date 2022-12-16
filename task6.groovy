job("Task-6-Job-2"){
    println('hello')
    steps{
        shell('''
                cd /workspace
                if kubectl get ns | grep web-server
                then
                    kubectl delete ns web-server
                fi
                kubectl create ns web-server
                if ls | grep .php
                then
                    kubectl create -f Deployments/web.yml -n web-server
                    kubectl create -f Deployments/service.yml -n web-server
                    sleep 15
                    podname=$(kubectl get pods -o=name -n web-server | grep phpserver | sed "s/^.\\{4\\}//")
                    kubectl cp index.php $podname:/var/www/html -n web-server
                    kubectl cp webdata $podname:/var/www/html -n web-server
                else
                    kubectl create -f Deployments/web-http.yml -n web-server
                    kubectl create -f Deployments/service-http.yml -n web-server
                    sleep 15
                    podname=$(kubectl get pods -o=name -n web-server | grep httpserver | sed "s/^.\\{4\\}//")
                    kubectl cp index.html $podname:/var/www/html -n web-server
                    kubectl cp webdata $podname:/var/www/html -n web-server
                fi 
            ''')
    }
    triggers {
        upstream('Task-6-Job-1', 'SUCCESS')
    }
}

job("Task-6-Job-3") {
    triggers {
            upstream {
                upstreamProjects('Task-6-Job-2')
                threshold('SUCCESS')
            }
        }
    steps {
        shell('''
if kubectl get pods -n web-server | grep phpserver
then
    status=$(curl -s -LI -w "%{http_code}" -o /dev/null 192.168.99.101:32323)
    
    if [ $status -eq 200 ];
    then
        echo "Website working"
        exit 1
    else
        exit 0
    fi
else
    status=$(curl -s -LI -w "%{http_code}" -o /dev/null 192.168.99.101:32324)
    if [ $status -eq 200 ];
    then
        echo "Website working"
        exit 1
    else
        exit 0
    fi
fi
''')
}
}
job("Task-6-Job-4"){
    publishers {
        extendedEmail {
            recipientList('vinodsharma@chingari.io')
            contentType('text/html')
            defaultSubject('Oops')
            defaultContent('Something broken')
            triggers {
                always {
                    subject('WebServer Issue')
                    content('Your WebServer has some error...Check the code and redeploy')
                    sendTo {
                        recipientList('1728102@kiit.ac.in')
                        developers()
                        requester()
                        culprits()
                    }
                }
            }
        }
    }
    triggers {
        upstream('Task-6-Job-3', 'SUCCESS')
   }
}
